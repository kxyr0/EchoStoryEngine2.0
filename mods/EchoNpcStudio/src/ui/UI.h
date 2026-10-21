#pragma once
#include "graphics/Graphics.h"
#include <functional>
namespace echo {
struct TextEdit {
    std::wstring value, composition;
    size_t caret = 0, anchor = 0;
    float offset = 0;
    wchar_t pendingSurrogate = 0;
    struct State {
        std::wstring text;
        size_t caret, anchor;
    };
    std::vector<State> undo, redo;
    void set(std::wstring text);
    void replace(const std::wstring &text);
    bool key(WPARAM key, bool ctrl, bool shift, HWND window);
    void character(wchar_t c);
    void copy(HWND window);
    void paste(HWND window);
    void history(bool forward);
    size_t previous(size_t p) const;
    size_t next(size_t p) const;
};
struct Widget {
    int id;
    D2D1_RECT_F bounds;
    bool enabled;
    TextEdit *edit;
    std::function<void()> action;
};
class UI {
    Graphics *graphics = nullptr;
    HWND window = nullptr;
    int pressed = -1, keyboardFocus = -1, hoverId = -1;
    bool selecting = false;
    TextEdit *focus = nullptr;
    ULONGLONG hoverSince = 0;
    std::wstring tooltip;
    D2D1_RECT_F tooltipBounds{};

  public:
    float mouseX = -1, mouseY = -1;
    std::vector<Widget> widgets;
    void initialize(HWND hwnd, Graphics &renderer) {
        window = hwnd;
        graphics = &renderer;
    }
    void begin() { widgets.clear(); }
    void button(int id, const std::wstring &title, D2D1_RECT_F rect, std::function<void()> action,
                bool enabled = true, bool selected = false);
    void toggle(int id, const std::wstring &title, D2D1_RECT_F rect, bool value,
                std::function<void()> action);
    void input(int id, TextEdit &edit, D2D1_RECT_F rect, bool enabled = true);
    bool down(float x, float y);
    bool up(float x, float y);
    bool move(float x, float y);
    bool key(WPARAM k);
    bool character(wchar_t c);
    bool ime(LPARAM flags);
    void clearFocus() {
        if (focus) {
            focus->composition.clear();
            focus->pendingSurrogate = 0;
        }
        focus = nullptr;
        selecting = false;
    }
    void focusInput(TextEdit &edit) {
        clearFocus();
        focus = &edit;
        edit.anchor = 0;
        edit.caret = edit.value.size();
    }
    void endComposition() {
        if (focus)
            focus->composition.clear();
    }
    bool hasFocus() const { return focus != nullptr; }
    void tab();
    void showTooltip();
};
inline D2D1_RECT_F rect(float x, float y, float w, float h) {
    return {x, y, x + w, y + h};
}
inline bool contains(D2D1_RECT_F r, float x, float y) {
    return x >= r.left && x < r.right && y >= r.top && y < r.bottom;
}
} // namespace echo
