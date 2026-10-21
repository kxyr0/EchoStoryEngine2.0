#include "ui/UI.h"
#include <imm.h>
namespace echo {
void TextEdit::set(std::wstring text) {
    value = std::move(text);
    caret = anchor = value.size();
    offset = 0;
    undo.clear();
    redo.clear();
    composition.clear();
    pendingSurrogate = 0;
}
size_t TextEdit::previous(size_t p) const {
    if (!p)
        return 0;
    --p;
    if (p && value[p] >= 0xdc00 && value[p] <= 0xdfff && value[p - 1] >= 0xd800 && value[p - 1] <= 0xdbff)
        --p;
    return p;
}
size_t TextEdit::next(size_t p) const {
    if (p >= value.size())
        return value.size();
    if (value[p] >= 0xd800 && value[p] <= 0xdbff && p + 1 < value.size() && value[p + 1] >= 0xdc00 &&
        value[p + 1] <= 0xdfff)
        return p + 2;
    return p + 1;
}
void TextEdit::replace(const std::wstring &text) {
    undo.push_back({value, caret, anchor});
    if (undo.size() > 256)
        undo.erase(undo.begin());
    redo.clear();
    size_t lo = std::min(caret, anchor), hi = std::max(caret, anchor);
    value.replace(lo, hi - lo, text);
    caret = anchor = lo + text.size();
}
void TextEdit::copy(HWND window) {
    if (caret == anchor)
        return;
    auto text = value.substr(std::min(caret, anchor), std::max(caret, anchor) - std::min(caret, anchor));
    if (!OpenClipboard(window))
        return;
    HGLOBAL memory = GlobalAlloc(GMEM_MOVEABLE, (text.size() + 1) * sizeof(wchar_t));
    if (memory) {
        auto p = GlobalLock(memory);
        if (p) {
            memcpy(p, text.c_str(), (text.size() + 1) * sizeof(wchar_t));
            GlobalUnlock(memory);
            EmptyClipboard();
            if (!SetClipboardData(CF_UNICODETEXT, memory))
                GlobalFree(memory);
        } else
            GlobalFree(memory);
    }
    CloseClipboard();
}
void TextEdit::paste(HWND window) {
    if (!OpenClipboard(window))
        return;
    HANDLE data = GetClipboardData(CF_UNICODETEXT);
    if (data) {
        auto p = static_cast<const wchar_t *>(GlobalLock(data));
        if (p) {
            std::wstring text(p);
            GlobalUnlock(data);
            text.erase(
                std::remove_if(text.begin(), text.end(), [](wchar_t c) { return c < L' ' || c == 0x7f; }),
                text.end());
            replace(text);
        }
    }
    CloseClipboard();
}
void TextEdit::history(bool forward) {
    auto &source = forward ? redo : undo;
    auto &destination = forward ? undo : redo;
    if (source.empty())
        return;
    destination.push_back({value, caret, anchor});
    auto s = std::move(source.back());
    source.pop_back();
    value = std::move(s.text);
    caret = s.caret;
    anchor = s.anchor;
}
bool TextEdit::key(WPARAM key, bool ctrl, bool shift, HWND window) {
    if (ctrl) {
        switch (key) {
        case 'A':
            anchor = 0;
            caret = value.size();
            return true;
        case 'C':
            copy(window);
            return true;
        case 'X':
            copy(window);
            if (caret != anchor)
                replace(L"");
            return true;
        case 'V':
            paste(window);
            return true;
        case 'Z':
            history(false);
            return true;
        case 'Y':
            history(true);
            return true;
        }
    }
    auto movement = [&](size_t p) {
        caret = p;
        if (!shift)
            anchor = caret;
    };
    switch (key) {
    case VK_LEFT:
        movement(!shift && caret != anchor ? std::min(caret, anchor) : previous(caret));
        return true;
    case VK_RIGHT:
        movement(!shift && caret != anchor ? std::max(caret, anchor) : next(caret));
        return true;
    case VK_HOME:
        movement(0);
        return true;
    case VK_END:
        movement(value.size());
        return true;
    case VK_BACK:
        if (caret == anchor)
            anchor = previous(caret);
        if (caret != anchor)
            replace(L"");
        return true;
    case VK_DELETE:
        if (caret == anchor)
            anchor = next(caret);
        if (caret != anchor)
            replace(L"");
        return true;
    }
    return false;
}
void TextEdit::character(wchar_t c) {
    if (c >= 0xd800 && c <= 0xdbff) {
        pendingSurrogate = c;
        return;
    }
    if (c >= 0xdc00 && c <= 0xdfff) {
        if (pendingSurrogate)
            replace(std::wstring{pendingSurrogate, c});
        pendingSurrogate = 0;
        return;
    }
    pendingSurrogate = 0;
    if (c >= L' ' && c != 0x7f)
        replace(std::wstring(1, c));
}
void UI::button(int id, const std::wstring &title, D2D1_RECT_F r, std::function<void()> action, bool enabled,
                bool selected) {
    bool hover = contains(r, mouseX, mouseY);
    graphics->fill(r,
                   !enabled        ? 0x20232A
                   : selected      ? 0x2585E6
                   : pressed == id ? 0x285783
                   : hover         ? 0x343A46
                                   : 0x272D37,
                   5);
    graphics->text(title, {r.left + 9, r.top + 7, r.right - 5, r.bottom}, enabled ? 0xE6E8EC : 0x646A76, 3);
    if (keyboardFocus == id && enabled) {
        graphics->line(r.left + 5, r.bottom - 2, r.right - 5, r.bottom - 2, 0x62AEFA, 2);
    }
    if (hover && !title.empty()) {
        if (hoverId != id) {
            hoverId = id;
            hoverSince = GetTickCount64();
        }
        tooltip = graphics->textWidth(title) > r.right - r.left - 14 ? title : L"";
        tooltipBounds = r;
    }
    widgets.push_back({id, r, enabled, nullptr, std::move(action)});
}
void UI::toggle(int id, const std::wstring &title, D2D1_RECT_F r, bool value, std::function<void()> action) {
    button(id, (value ? tr("ui.control.checked") : tr("ui.control.unchecked")) + title, r, std::move(action),
           true, false);
}
void UI::input(int id, TextEdit &edit, D2D1_RECT_F r, bool enabled) {
    bool focused = focus == &edit;
    graphics->fill(r, focused ? 0x2585E6 : 0x343A46, 4);
    graphics->fill({r.left + 1, r.top + 1, r.right - 1, r.bottom - 1}, enabled ? 0x101216 : 0x20232A, 3);
    graphics->clip({r.left + 7, r.top + 2, r.right - 7, r.bottom - 2});
    float cursor = graphics->textWidth(edit.value.substr(0, edit.caret)), available = r.right - r.left - 18;
    if (focused) {
        if (cursor - edit.offset > available)
            edit.offset = cursor - available;
        if (cursor < edit.offset)
            edit.offset = cursor;
    }
    float x = r.left + 9 - edit.offset, y = r.top + 7;
    if (focused && edit.caret != edit.anchor) {
        float a = graphics->textWidth(edit.value.substr(0, std::min(edit.caret, edit.anchor))),
              b = graphics->textWidth(edit.value.substr(0, std::max(edit.caret, edit.anchor)));
        graphics->fill({x + a, y, x + b, y + 18}, 0x285783);
    }
    graphics->text(edit.value, {x, y, x + std::max(10000.f, available), r.bottom},
                   enabled ? 0xE6E8EC : 0x969BA6);
    if (focused) {
        graphics->line(x + cursor, y, x + cursor, y + 18, 0xE6E8EC);
        if (!edit.composition.empty()) {
            graphics->text(edit.composition, {x + cursor, y, x + cursor + 500, r.bottom}, 0x62AEFA);
            graphics->line(x + cursor, y + 18, x + cursor + graphics->textWidth(edit.composition), y + 18,
                           0x62AEFA);
        }
    }
    graphics->unclip();
    widgets.push_back({id, r, enabled, &edit, {}});
}
bool UI::down(float x, float y) {
    mouseX = x;
    mouseY = y;
    for (auto it = widgets.rbegin(); it != widgets.rend(); ++it) {
        auto &w = *it;
        if (!contains(w.bounds, x, y))
            continue;
        if (!w.enabled)
            return true;
        pressed = w.id;
        keyboardFocus = w.id;
        if (w.edit) {
            if (focus != w.edit)
                clearFocus();
            focus = w.edit;
            size_t p = graphics->textPosition(focus->value, x - w.bounds.left - 9 + focus->offset);
            focus->caret = p;
            if (!(GetKeyState(VK_SHIFT) & 0x8000))
                focus->anchor = p;
            selecting = true;
            HIMC imc = ImmGetContext(window);
            if (imc) {
                COMPOSITIONFORM form{};
                form.dwStyle = CFS_POINT;
                float dpi = GetDpiForWindow(window) / 96.f;
                form.ptCurrentPos = {LONG((w.bounds.left + 9) * dpi), LONG(w.bounds.bottom * dpi)};
                ImmSetCompositionWindow(imc, &form);
                ImmReleaseContext(window, imc);
            }
        } else
            clearFocus();
        SetCapture(window);
        return true;
    }
    clearFocus();
    keyboardFocus = -1;
    return false;
}
bool UI::up(float x, float y) {
    auto id = pressed;
    pressed = -1;
    selecting = false;
    if (GetCapture() == window)
        ReleaseCapture();
    for (auto &w : widgets)
        if (w.id == id && contains(w.bounds, x, y) && w.enabled) {
            auto action = w.action;
            if (action)
                action();
            return true;
        }
    return id != -1;
}
bool UI::move(float x, float y) {
    mouseX = x;
    mouseY = y;
    if (selecting && focus) {
        for (auto &w : widgets)
            if (w.edit == focus) {
                focus->caret = graphics->textPosition(focus->value, x - w.bounds.left - 9 + focus->offset);
                break;
            }
        return true;
    }
    return false;
}
bool UI::key(WPARAM k) {
    if (k == VK_TAB) {
        tab();
        return true;
    }
    if (!focus && (k == VK_RETURN || k == VK_SPACE)) {
        for (auto &w : widgets)
            if (w.id == keyboardFocus && w.enabled && w.action) {
                auto action = w.action;
                action();
                return true;
            }
    }
    return focus && focus->key(k, (GetKeyState(VK_CONTROL) & 0x8000) != 0,
                               (GetKeyState(VK_SHIFT) & 0x8000) != 0, window);
}
bool UI::character(wchar_t c) {
    if (!focus)
        return false;
    focus->character(c);
    return true;
}
bool UI::ime(LPARAM flags) {
    if (!focus)
        return false;
    HIMC imc = ImmGetContext(window);
    if (!imc)
        return false;
    auto get = [&](DWORD type) {
        LONG count = ImmGetCompositionStringW(imc, type, nullptr, 0);
        if (count <= 0)
            return std::wstring{};
        std::wstring out(size_t(count) / 2, L' ');
        ImmGetCompositionStringW(imc, type, out.data(), count);
        return out;
    };
    if (flags & GCS_RESULTSTR) {
        focus->replace(get(GCS_RESULTSTR));
        focus->composition.clear();
    }
    if (flags & GCS_COMPSTR)
        focus->composition = get(GCS_COMPSTR);
    ImmReleaseContext(window, imc);
    return true;
}
void UI::tab() {
    std::vector<Widget *> entries;
    for (auto &w : widgets)
        if (w.enabled)
            entries.push_back(&w);
    if (entries.empty())
        return;
    auto it = std::find_if(entries.begin(), entries.end(), [&](Widget *w) { return w->id == keyboardFocus; });
    int index = it == entries.end() ? -1 : int(it - entries.begin());
    bool backwards = (GetKeyState(VK_SHIFT) & 0x8000) != 0;
    index = (index + (backwards ? -1 : 1) + int(entries.size())) % int(entries.size());
    clearFocus();
    keyboardFocus = entries[index]->id;
    focus = entries[index]->edit;
}
void UI::showTooltip() {
    if (tooltip.empty() || !contains(tooltipBounds, mouseX, mouseY) || GetTickCount64() - hoverSince < 800)
        return;
    float w = graphics->textWidth(tooltip) + 18;
    graphics->fill(rect(tooltipBounds.left, tooltipBounds.bottom + 5, w, 29), 0x343A46, 4);
    graphics->text(tooltip, rect(tooltipBounds.left + 9, tooltipBounds.bottom + 11, w - 9, 20));
}
} // namespace echo
