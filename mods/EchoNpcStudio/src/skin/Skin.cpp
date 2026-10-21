#include "skin/Skin.h"
#include <wincodec.h>
namespace echo {
bool validSkinSize(unsigned w, unsigned h) {
    return w == h && w >= 64 && w <= 4096 && w % 64 == 0;
}
Image loadPng(const fs::path &path, bool skinLayout) {
    ComPtr<IWICImagingFactory> factory;
    check(CoCreateInstance(CLSID_WICImagingFactory, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&factory)),
          tr8("error.skin.wic_factory"));
    ComPtr<IWICBitmapDecoder> decoder;
    check(factory->CreateDecoderFromFilename(path.c_str(), nullptr, GENERIC_READ,
                                             WICDecodeMetadataCacheOnLoad, &decoder),
          tr8("error.skin.open_png"));
    GUID format{};
    check(decoder->GetContainerFormat(&format), tr8("error.skin.image_format"));
    if (format != GUID_ContainerFormatPng)
        throw std::runtime_error(tr8("error.skin.file_is_not_png"));
    ComPtr<IWICBitmapFrameDecode> frame;
    check(decoder->GetFrame(0, &frame), tr8("error.skin.png_frame"));
    Image result;
    check(frame->GetSize(&result.width, &result.height), tr8("error.skin.png_size"));
    if (!result.width || !result.height || result.width > 4096 || result.height > 4096)
        throw std::runtime_error(tr8("error.skin.invalid_png_size_1_4096"));
    if (skinLayout && !validSkinSize(result.width, result.height))
        throw std::runtime_error(tr8("error.skin.skin_must_use_a_square_64x64_layout_64_4096_multiple"));
    ComPtr<IWICFormatConverter> converter;
    check(factory->CreateFormatConverter(&converter), tr8("error.skin.wic_converter"));
    check(converter->Initialize(frame.Get(), GUID_WICPixelFormat32bppRGBA, WICBitmapDitherTypeNone, nullptr,
                                0, WICBitmapPaletteTypeCustom),
          tr8("error.skin.convert_png"));
    result.rgba.resize(size_t(result.width) * result.height * 4);
    check(converter->CopyPixels(nullptr, result.width * 4, static_cast<UINT>(result.rgba.size()),
                                result.rgba.data()),
          tr8("error.skin.png_pixels"));
    return result;
}
void savePng(const fs::path &path, const Image &image) {
    ComPtr<IWICImagingFactory> factory;
    check(CoCreateInstance(CLSID_WICImagingFactory, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&factory)),
          tr8("error.skin.wic_factory"));
    ComPtr<IWICStream> stream;
    check(factory->CreateStream(&stream), tr8("error.skin.wic_stream"));
    check(stream->InitializeFromFilename(path.c_str(), GENERIC_WRITE), tr8("error.skin.write_png"));
    ComPtr<IWICBitmapEncoder> encoder;
    check(factory->CreateEncoder(GUID_ContainerFormatPng, nullptr, &encoder), tr8("error.skin.png_encoder"));
    check(encoder->Initialize(stream.Get(), WICBitmapEncoderNoCache), tr8("error.skin.encoder_initialize"));
    ComPtr<IWICBitmapFrameEncode> frame;
    check(encoder->CreateNewFrame(&frame, nullptr), tr8("error.skin.encoder_frame"));
    check(frame->Initialize(nullptr), tr8("error.skin.frame_initialize"));
    check(frame->SetSize(image.width, image.height), tr8("error.skin.frame_size"));
    WICPixelFormatGUID format = GUID_WICPixelFormat32bppBGRA;
    check(frame->SetPixelFormat(&format), tr8("error.skin.pixel_format"));
    if (format != GUID_WICPixelFormat32bppBGRA)
        throw std::runtime_error(tr8("error.skin.bgra_encoding_unavailable"));
    auto pixels = image.rgba;
    for (size_t i = 0; i < pixels.size(); i += 4)
        std::swap(pixels[i], pixels[i + 2]);
    check(frame->WritePixels(image.height, image.width * 4, static_cast<UINT>(pixels.size()), pixels.data()),
          tr8("error.skin.write_pixels"));
    check(frame->Commit(), tr8("error.skin.frame_commit"));
    check(encoder->Commit(), tr8("error.skin.png_commit"));
}
Image makePortrait(const Image &image, bool hat) {
    if (!validSkinSize(image.width, image.height))
        throw std::runtime_error(tr8("error.skin.invalid_portrait_source"));
    unsigned s = image.width / 64;
    Image out{8 * s, 8 * s, {}};
    out.rgba.resize(size_t(out.width) * out.height * 4);
    for (unsigned y = 0; y < out.height; ++y)
        for (unsigned x = 0; x < out.width; ++x) {
            auto base = &image.rgba[((y + 8 * s) * image.width + x + 8 * s) * 4];
            auto layer = &image.rgba[((y + 8 * s) * image.width + x + 40 * s) * 4];
            auto dst = &out.rgba[(y * out.width + x) * 4];
            float a = hat ? layer[3] / 255.f : 0, b = base[3] / 255.f, alpha = a + b * (1 - a);
            for (int c = 0; c < 3; ++c)
                dst[c] =
                    alpha > 0
                        ? static_cast<uint8_t>(std::lround((layer[c] * a + base[c] * b * (1 - a)) / alpha))
                        : 0;
            dst[3] = static_cast<uint8_t>(std::lround(alpha * 255));
        }
    return out;
}
std::shared_ptr<Skin> loadSkin(const fs::path &path) {
    auto skin = std::make_shared<Skin>();
    skin->path = path;
    skin->image = loadPng(path);
    skin->portrait = makePortrait(skin->image);
    skin->basePortrait = makePortrait(skin->image, false);
    return skin;
}
EyeMask detectEyes(const Image &face) {
    EyeMask out;
    out.left.resize(size_t(face.width) * face.height);
    out.right = out.left;
    auto luminance = [&](unsigned x, unsigned y) {
        auto p = &face.rgba[(y * face.width + x) * 4];
        return (p[0] * .2126f + p[1] * .7152f + p[2] * .0722f);
    };
    unsigned s = face.width / 8;
    float best = 0;
    unsigned row = 4 * s;
    for (unsigned y = 3 * s; y < 6 * s; ++y) {
        float score = 0;
        for (unsigned x = s; x < 3 * s; ++x) {
            float a = luminance(x, y), b = luminance(face.width - 1 - x, y);
            float skin = (luminance(x, y - 1) + luminance(x, y + 1)) / 2;
            score += std::max(0.f, skin - a) + std::max(0.f, skin - b) - std::abs(a - b) * .3f;
        }
        if (score > best) {
            best = score;
            row = y;
        }
    }
    if (best > 20 * s) {
        for (unsigned x = s; x < 3 * s; ++x) {
            out.left[row * face.width + x] = 1;
            out.right[row * face.width + face.width - 1 - x] = 1;
        }
        out.confidence = std::min(.8f, best / (400 * s));
    }
    return out;
}
Image maskImage(const Image &face, const std::vector<uint8_t> &mask) {
    Image out = face;
    for (size_t i = 0; i < mask.size(); ++i)
        if (!mask[i])
            out.rgba[i * 4 + 3] = 0;
    return out;
}
} // namespace echo
