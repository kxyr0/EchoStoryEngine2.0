#include "app/Application.h"
namespace echo {
void Application::openNameColorPicker() {
    if (selected < 0 || imageLoading || scanning)
        return;
    sync();
    const auto &n = npcs[selected];
    const auto &skin = n.skin->image;
    unsigned factor = skin.width / 64;
    auto build = [&](bool back) {
        Image out{16 * factor, 32 * factor, std::vector<uint8_t>(16 * 32 * factor * factor * 4)};
        auto part = [&](int dx, int dy, int w, int h, int depth, int u, int v, int ou, int ov, bool overlay) {
            int tx = u + (back ? 2 * depth + w : depth), ty = v + depth;
            int ox = ou + (back ? 2 * depth + w : depth), oy = ov + depth;
            for (unsigned y = 0; y < unsigned(h) * factor; ++y)
                for (unsigned x = 0; x < unsigned(w) * factor; ++x) {
                    auto base = &skin.rgba[((ty * factor + y) * skin.width + tx * factor + x) * 4];
                    auto layer = &skin.rgba[((oy * factor + y) * skin.width + ox * factor + x) * 4];
                    auto dst = &out.rgba[((dy * factor + y) * out.width + dx * factor + x) * 4];
                    double a = overlay ? layer[3] / 255. : 0, b = base[3] / 255., alpha = a + b * (1 - a);
                    for (int c = 0; c < 3; ++c)
                        dst[c] =
                            alpha ? uint8_t(std::lround((layer[c] * a + base[c] * b * (1 - a)) / alpha)) : 0;
                    dst[3] = uint8_t(std::lround(alpha * 255));
                }
        };
        int arm = n.slim ? 3 : 4;
        part(4, 0, 8, 8, 8, 0, 0, 32, 0, n.hat);
        part(4, 8, 8, 12, 4, 16, 16, 16, 32, n.jacket);
        part(back ? 12 : 4 - arm, 8, arm, 12, 4, 40, 16, 40, 32, n.sleeves);
        part(back ? 4 - arm : 12, 8, arm, 12, 4, 32, 48, 48, 48, n.sleeves);
        part(back ? 8 : 4, 20, 4, 12, 4, 0, 16, 0, 32, n.pants);
        part(back ? 4 : 8, 20, 4, 12, 4, 16, 48, 0, 48, n.pants);
        return out;
    };
    clothingFront = build(false);
    clothingBack = build(true);
    graphics.invalidateImages();
    nameColorPicker = true;
    modelDropdown = false;
    contextIndex = -1;
    ui.clearFocus();
}
void Application::drawNameColorPicker() {
    ui.widgets.clear();
    float x = (width - 920) / 2, y = (height - 580) / 2;
    graphics.fill(rect(0, 58, width, height - 58), 0x101216);
    graphics.fill(rect(x - 3, y - 3, 926, 586), 0x343A46, 8);
    graphics.fill(rect(x, y, 920, 580), 0x191C22, 6);
    graphics.text(tr("ui.namecolorpicker.name_color_from_clothing_and_skin"), rect(x + 20, y + 16, 880, 28),
                  0xE6E8EC, 1);
    graphics.text(
        tr("ui.namecolorpicker.click_a_pixel_colors_are_sampled_without_lighting_transparent_areas"),
        rect(x + 20, y + 52, 880, 26), 0x969BA6);
    graphics.text(tr("ui.namecolorpicker.front"), rect(x + 20, y + 90, 180, 22), 0x969BA6);
    graphics.text(tr("ui.namecolorpicker.back"), rect(x + 224, y + 90, 180, 22), 0x969BA6);
    graphics.text(tr("ui.namecolorpicker.full_texture_including_side_faces"), rect(x + 450, y + 90, 450, 22),
                  0x969BA6);
    clothingFrontRect = rect(x + 20, y + 118, 192, 384);
    clothingBackRect = rect(x + 224, y + 118, 192, 384);
    colorAtlasRect = rect(x + 450, y + 118, 384, 384);
    for (auto r : {clothingFrontRect, clothingBackRect, colorAtlasRect})
        graphics.fill(r, 0x272D37);
    graphics.image(clothingFront, clothingFrontRect);
    graphics.image(clothingBack, clothingBackRect);
    graphics.image(npcs[selected].skin->image, colorAtlasRect);
    graphics.text(tr("ui.namecolorpicker.current_color") + color.value, rect(x + 20, y + 527, 550, 28));
    ui.button(460, tr("ui.facewizard.cancel"), rect(x + 785, y + 527, 115, 32), [this] {
        nameColorPicker = false;
        ui.clearFocus();
    });
}
bool Application::nameColorDown(float x, float y) {
    if (!nameColorPicker)
        return false;
    const Image *image = nullptr;
    D2D1_RECT_F bounds{};
    if (contains(clothingFrontRect, x, y)) {
        image = &clothingFront;
        bounds = clothingFrontRect;
    } else if (contains(clothingBackRect, x, y)) {
        image = &clothingBack;
        bounds = clothingBackRect;
    } else if (contains(colorAtlasRect, x, y)) {
        image = &npcs[selected].skin->image;
        bounds = colorAtlasRect;
    }
    if (!image)
        return true;
    unsigned px =
        std::min(image->width - 1, unsigned((x - bounds.left) * image->width / (bounds.right - bounds.left)));
    unsigned py = std::min(image->height - 1,
                           unsigned((y - bounds.top) * image->height / (bounds.bottom - bounds.top)));
    auto pixel = &image->rgba[(size_t(py) * image->width + px) * 4];
    if (!pixel[3]) {
        status = tr("ui.namecolorpicker.choose_an_opaque_clothing_pixel");
        return true;
    }
    wchar_t hex[8];
    swprintf_s(hex, L"#%02X%02X%02X", pixel[0], pixel[1], pixel[2]);
    color.set(hex);
    sync();
    nameColorPicker = false;
    ui.clearFocus();
    status = tr("ui.namecolorpicker.name_color_sampled_from_skin") + color.value;
    return true;
}
} // namespace echo
