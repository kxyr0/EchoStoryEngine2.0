#include "graphics/Graphics.h"
#include "Shader.h"
#include <d3dcompiler.h>
#include <array>
namespace echo {
using namespace DirectX;
static D2D1_COLOR_F colorOf(uint32_t c) {
    return D2D1::ColorF((c >> 16 & 255) / 255.f, (c >> 8 & 255) / 255.f, (c & 255) / 255.f);
}
void Graphics::initialize(HWND hwnd, bool forceWarp) {
    window = hwnd;
    warp = forceWarp;
    UINT flags = D3D11_CREATE_DEVICE_BGRA_SUPPORT;
    D3D_FEATURE_LEVEL levels[] = {D3D_FEATURE_LEVEL_11_0, D3D_FEATURE_LEVEL_10_1, D3D_FEATURE_LEVEL_10_0};
    HRESULT hr = D3D11CreateDevice(nullptr, forceWarp ? D3D_DRIVER_TYPE_WARP : D3D_DRIVER_TYPE_HARDWARE,
                                   nullptr, flags, levels, 3, D3D11_SDK_VERSION, &device, nullptr, &context);
    if (FAILED(hr) && !forceWarp) {
        warp = true;
        hr = D3D11CreateDevice(nullptr, D3D_DRIVER_TYPE_WARP, nullptr, flags, levels, 3, D3D11_SDK_VERSION,
                               &device, nullptr, &context);
    }
    check(hr, tr8("error.graphics.direct3d_11_device"));
    ComPtr<IDXGIDevice> dxgi;
    check(device.As(&dxgi), tr8("error.graphics.dxgi_device"));
    ComPtr<IDXGIAdapter> adapter;
    check(dxgi->GetAdapter(&adapter), tr8("error.graphics.dxgi_adapter"));
    ComPtr<IDXGIFactory2> dxgiFactory;
    check(adapter->GetParent(IID_PPV_ARGS(&dxgiFactory)), tr8("error.graphics.dxgi_factory"));
    DXGI_SWAP_CHAIN_DESC1 desc{};
    desc.Format = DXGI_FORMAT_B8G8R8A8_UNORM;
    desc.SampleDesc.Count = 1;
    desc.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
    desc.BufferCount = 2;
    desc.SwapEffect = DXGI_SWAP_EFFECT_FLIP_SEQUENTIAL;
    check(dxgiFactory->CreateSwapChainForHwnd(device.Get(), window, &desc, nullptr, nullptr, &swapChain),
          tr8("error.graphics.swap_chain"));
    check(dxgiFactory->MakeWindowAssociation(window, DXGI_MWA_NO_ALT_ENTER),
          tr8("error.graphics.window_association"));
    D2D1_FACTORY_OPTIONS options{};
    check(D2D1CreateFactory(D2D1_FACTORY_TYPE_SINGLE_THREADED, __uuidof(ID2D1Factory1), &options,
                            reinterpret_cast<void **>(factory.GetAddressOf())),
          tr8("error.graphics.direct2d_factory"));
    check(factory->CreateDevice(dxgi.Get(), &d2dDevice), tr8("error.graphics.direct2d_device"));
    check(d2dDevice->CreateDeviceContext(D2D1_DEVICE_CONTEXT_OPTIONS_NONE, &canvas),
          tr8("error.graphics.direct2d_context"));
    check(canvas->CreateSolidColorBrush(colorOf(0xffffff), &brush), tr8("error.graphics.ui_brush"));
    check(DWriteCreateFactory(DWRITE_FACTORY_TYPE_SHARED, __uuidof(IDWriteFactory),
                              reinterpret_cast<IUnknown **>(textFactory.GetAddressOf())),
          tr8("error.graphics.directwrite_factory"));
    check(textFactory->CreateTextFormat(L"Segoe UI", nullptr, DWRITE_FONT_WEIGHT_NORMAL,
                                        DWRITE_FONT_STYLE_NORMAL, DWRITE_FONT_STRETCH_NORMAL, 13,
                                        tr("locale.windows_tag").c_str(), &font),
          tr8("error.graphics.ui_font"));
    check(textFactory->CreateTextFormat(L"Segoe UI", nullptr, DWRITE_FONT_WEIGHT_SEMI_BOLD,
                                        DWRITE_FONT_STYLE_NORMAL, DWRITE_FONT_STRETCH_NORMAL, 16,
                                        tr("locale.windows_tag").c_str(), &heading),
          tr8("error.graphics.heading_font"));
    check(textFactory->CreateTextFormat(L"Consolas", nullptr, DWRITE_FONT_WEIGHT_NORMAL,
                                        DWRITE_FONT_STYLE_NORMAL, DWRITE_FONT_STRETCH_NORMAL, 13,
                                        tr("locale.windows_tag").c_str(), &mono),
          tr8("error.graphics.code_font"));
    pipeline();
    RECT r;
    GetClientRect(hwnd, &r);
    resize(r.right, r.bottom, static_cast<float>(GetDpiForWindow(hwnd)));
}
void Graphics::pipeline() {
    ComPtr<ID3DBlob> vertexCode, pixelCode, error;
    HRESULT hr = D3DCompile(skinShader, sizeof(skinShader) - 1, "Minecraft.hlsl", nullptr, nullptr, "VSMain",
                            "vs_4_0", D3DCOMPILE_ENABLE_STRICTNESS, 0, &vertexCode, &error);
    if (FAILED(hr))
        throw std::runtime_error(error ? static_cast<char *>(error->GetBufferPointer())
                                       : tr8("error.graphics.vertex_shader_compilation_failed"));
    check(D3DCompile(skinShader, sizeof(skinShader) - 1, "Minecraft.hlsl", nullptr, nullptr, "PSMain",
                     "ps_4_0", D3DCOMPILE_ENABLE_STRICTNESS, 0, &pixelCode, &error),
          tr8("error.graphics.pixel_shader_compile"));
    check(
        device->CreateVertexShader(vertexCode->GetBufferPointer(), vertexCode->GetBufferSize(), nullptr, &vs),
        tr8("error.graphics.vertex_shader"));
    check(device->CreatePixelShader(pixelCode->GetBufferPointer(), pixelCode->GetBufferSize(), nullptr, &ps),
          tr8("error.graphics.pixel_shader"));
    D3D11_INPUT_ELEMENT_DESC layout[] = {
        {"POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0, D3D11_INPUT_PER_VERTEX_DATA, 0},
        {"NORMAL", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 12, D3D11_INPUT_PER_VERTEX_DATA, 0},
        {"TEXCOORD", 0, DXGI_FORMAT_R32G32_FLOAT, 0, 24, D3D11_INPUT_PER_VERTEX_DATA, 0}};
    check(device->CreateInputLayout(layout, 3, vertexCode->GetBufferPointer(), vertexCode->GetBufferSize(),
                                    &inputLayout),
          tr8("error.graphics.vertex_layout"));
    D3D11_BUFFER_DESC b{};
    b.ByteWidth = sizeof(Vertex) * 432;
    b.Usage = D3D11_USAGE_DYNAMIC;
    b.BindFlags = D3D11_BIND_VERTEX_BUFFER;
    b.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
    check(device->CreateBuffer(&b, nullptr, &vertices), tr8("error.graphics.vertex_buffer"));
    b.ByteWidth = sizeof(XMFLOAT4X4);
    b.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
    check(device->CreateBuffer(&b, nullptr, &constants), tr8("error.graphics.camera_buffer"));
    D3D11_SAMPLER_DESC s{};
    s.Filter = D3D11_FILTER_MIN_MAG_MIP_POINT;
    s.AddressU = s.AddressV = s.AddressW = D3D11_TEXTURE_ADDRESS_CLAMP;
    s.MaxLOD = D3D11_FLOAT32_MAX;
    check(device->CreateSamplerState(&s, &sampler), tr8("error.graphics.pixel_sampler"));
    D3D11_RASTERIZER_DESC r{};
    r.FillMode = D3D11_FILL_SOLID;
    r.CullMode = D3D11_CULL_NONE;
    r.DepthClipEnable = TRUE;
    r.ScissorEnable = TRUE;
    check(device->CreateRasterizerState(&r, &raster), tr8("error.graphics.rasterizer"));
    D3D11_BLEND_DESC blendDesc{};
    auto &t = blendDesc.RenderTarget[0];
    t.BlendEnable = TRUE;
    t.SrcBlend = D3D11_BLEND_SRC_ALPHA;
    t.DestBlend = D3D11_BLEND_INV_SRC_ALPHA;
    t.BlendOp = D3D11_BLEND_OP_ADD;
    t.SrcBlendAlpha = D3D11_BLEND_ONE;
    t.DestBlendAlpha = D3D11_BLEND_INV_SRC_ALPHA;
    t.BlendOpAlpha = D3D11_BLEND_OP_ADD;
    t.RenderTargetWriteMask = D3D11_COLOR_WRITE_ENABLE_ALL;
    check(device->CreateBlendState(&blendDesc, &blend), tr8("error.graphics.alpha_blending"));
    D3D11_DEPTH_STENCIL_DESC depth{};
    depth.DepthEnable = TRUE;
    depth.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ALL;
    depth.DepthFunc = D3D11_COMPARISON_LESS_EQUAL;
    check(device->CreateDepthStencilState(&depth, &depthWrite), tr8("error.graphics.depth_write_state"));
    depth.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ZERO;
    check(device->CreateDepthStencilState(&depth, &depthRead), tr8("error.graphics.overlay_depth_state"));
}
void Graphics::targets() {
    ComPtr<ID3D11Texture2D> back;
    check(swapChain->GetBuffer(0, IID_PPV_ARGS(&back)), tr8("error.graphics.back_buffer"));
    check(device->CreateRenderTargetView(back.Get(), nullptr, &renderTarget),
          tr8("error.graphics.render_target"));
    D3D11_TEXTURE2D_DESC d{};
    d.Width = width;
    d.Height = height;
    d.MipLevels = d.ArraySize = 1;
    d.Format = DXGI_FORMAT_D24_UNORM_S8_UINT;
    d.SampleDesc.Count = 1;
    d.BindFlags = D3D11_BIND_DEPTH_STENCIL;
    ComPtr<ID3D11Texture2D> depth;
    check(device->CreateTexture2D(&d, nullptr, &depth), tr8("error.graphics.depth_buffer"));
    check(device->CreateDepthStencilView(depth.Get(), nullptr, &depthTarget),
          tr8("error.graphics.depth_target"));
    ComPtr<IDXGISurface> dxgiSurface;
    check(back.As(&dxgiSurface), tr8("error.graphics.shared_surface"));
    auto properties = D2D1::BitmapProperties1(
        D2D1_BITMAP_OPTIONS_TARGET | D2D1_BITMAP_OPTIONS_CANNOT_DRAW,
        D2D1::PixelFormat(DXGI_FORMAT_B8G8R8A8_UNORM, D2D1_ALPHA_MODE_IGNORE), dpi, dpi);
    check(canvas->CreateBitmapFromDxgiSurface(dxgiSurface.Get(), &properties, &surface),
          tr8("error.graphics.direct2d_swap_chain_bitmap"));
    canvas->SetTarget(surface.Get());
    canvas->SetDpi(dpi, dpi);
}
void Graphics::resize(unsigned w, unsigned h, float dpiValue) {
    if (!w || !h)
        return;
    dpi = dpiValue;
    if (w == width && h == height) {
        canvas->SetDpi(dpi, dpi);
        return;
    }
    canvas->SetTarget(nullptr);
    surface.Reset();
    renderTarget.Reset();
    depthTarget.Reset();
    context->OMSetRenderTargets(0, nullptr, nullptr);
    context->Flush();
    check(swapChain->ResizeBuffers(0, w, h, DXGI_FORMAT_UNKNOWN, 0), tr8("error.graphics.resize_swap_chain"));
    width = w;
    height = h;
    targets();
}
void Graphics::setSkin(std::shared_ptr<Skin> skin) {
    faceTextures.clear();
    activeSkin = std::move(skin);
    skinTexture.Reset();
    if (!activeSkin)
        return;
    auto &img = activeSkin->image;
    D3D11_TEXTURE2D_DESC d{};
    d.Width = img.width;
    d.Height = img.height;
    d.MipLevels = d.ArraySize = 1;
    d.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    d.SampleDesc.Count = 1;
    d.Usage = D3D11_USAGE_IMMUTABLE;
    d.BindFlags = D3D11_BIND_SHADER_RESOURCE;
    D3D11_SUBRESOURCE_DATA source{img.rgba.data(), img.width * 4, 0};
    ComPtr<ID3D11Texture2D> texture;
    check(device->CreateTexture2D(&d, &source, &texture), tr8("error.graphics.skin_gpu_texture"));
    check(device->CreateShaderResourceView(texture.Get(), nullptr, &skinTexture),
          tr8("error.graphics.skin_texture_view"));
}
void Graphics::begin() {
    const float clear[] = {.063f, .071f, .086f, 1};
    context->ClearRenderTargetView(renderTarget.Get(), clear);
    context->ClearDepthStencilView(depthTarget.Get(), D3D11_CLEAR_DEPTH | D3D11_CLEAR_STENCIL, 1, 0);
}
void Graphics::model(D2D1_RECT_F bounds, const ModelSettings &settings, float yaw, float pitch,
                     float distance, bool head, const FaceRig *faceRig) {
    if (!skinTexture)
        return;
    float scale = dpi / 96.f;
    D3D11_VIEWPORT viewport{bounds.left * scale,
                            bounds.top * scale,
                            (bounds.right - bounds.left) * scale,
                            (bounds.bottom - bounds.top) * scale,
                            0,
                            1};
    if (viewport.Width < 1 || viewport.Height < 1)
        return;
    context->RSSetViewports(1, &viewport);
    D3D11_RECT scissor{static_cast<LONG>(viewport.TopLeftX), static_cast<LONG>(viewport.TopLeftY),
                       static_cast<LONG>(viewport.TopLeftX + viewport.Width),
                       static_cast<LONG>(viewport.TopLeftY + viewport.Height)};
    context->RSSetScissorRects(1, &scissor);
    context->RSSetState(raster.Get());
    context->OMSetRenderTargets(1, renderTarget.GetAddressOf(), depthTarget.Get());
    context->OMSetBlendState(blend.Get(), nullptr, 0xffffffff);
    float targetY = head ? 28.f : 16.f;
    float dist = head ? distance * .35f : distance;
    XMVECTOR target = XMVectorSet(0, targetY, 0, 1);
    XMVECTOR eye =
        XMVectorAdd(target, XMVectorSet(std::sin(yaw) * std::cos(pitch) * dist, std::sin(pitch) * dist,
                                        -std::cos(yaw) * std::cos(pitch) * dist, 0));
    auto matrix =
        XMMatrixLookAtLH(eye, target, XMVectorSet(0, 1, 0, 0)) *
        XMMatrixPerspectiveFovLH(XMConvertToRadians(38), viewport.Width / viewport.Height, .1f, 500);
    D3D11_MAPPED_SUBRESOURCE mapped{};
    check(context->Map(constants.Get(), 0, D3D11_MAP_WRITE_DISCARD, 0, &mapped),
          tr8("error.graphics.map_camera"));
    XMStoreFloat4x4(static_cast<XMFLOAT4X4 *>(mapped.pData), matrix);
    context->Unmap(constants.Get(), 0);
    context->IASetInputLayout(inputLayout.Get());
    context->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
    UINT stride = sizeof(Vertex), offset = 0;
    context->IASetVertexBuffers(0, 1, vertices.GetAddressOf(), &stride, &offset);
    context->VSSetShader(vs.Get(), nullptr, 0);
    context->VSSetConstantBuffers(0, 1, constants.GetAddressOf());
    context->PSSetShader(ps.Get(), nullptr, 0);
    context->PSSetShaderResources(0, 1, skinTexture.GetAddressOf());
    context->PSSetSamplers(0, 1, sampler.GetAddressOf());
    for (bool outer : {false, true}) {
        context->PSSetShaderResources(0, 1, skinTexture.GetAddressOf());
        auto mesh = buildModel(settings, outer);
        if (outer) { // Sort translucent faces back-to-front, retaining depth from the opaque model.
            XMFLOAT3 camera;
            XMStoreFloat3(&camera, eye);
            std::vector<std::array<Vertex, 6>> faces(mesh.size() / 6);
            for (size_t i = 0; i < faces.size(); ++i)
                std::copy_n(mesh.begin() + i * 6, 6, faces[i].begin());
            auto depth = [&](const auto &face) {
                float x = 0, y = 0, z = 0;
                for (auto &vert : face) {
                    x += vert.position.x;
                    y += vert.position.y;
                    z += vert.position.z;
                }
                x = x / 6 - camera.x;
                y = y / 6 - camera.y;
                z = z / 6 - camera.z;
                return x * x + y * y + z * z;
            };
            std::sort(faces.begin(), faces.end(),
                      [&](const auto &a, const auto &b) { return depth(a) > depth(b); });
            mesh.clear();
            for (auto &face : faces)
                mesh.insert(mesh.end(), face.begin(), face.end());
        }
        if (mesh.empty())
            continue;
        check(context->Map(vertices.Get(), 0, D3D11_MAP_WRITE_DISCARD, 0, &mapped),
              tr8("error.graphics.map_model"));
        memcpy(mapped.pData, mesh.data(), mesh.size() * sizeof(Vertex));
        context->Unmap(vertices.Get(), 0);
        context->OMSetDepthStencilState(outer ? depthRead.Get() : depthWrite.Get(), 0);
        context->Draw(static_cast<UINT>(mesh.size()), 0);
        if (!outer && faceRig) {
            // Separate authored layers, preserving original skin and fractional placement.
            context->OMSetDepthStencilState(depthRead.Get(), 0);
            for (auto &layer : buildFaceLayers(*faceRig)) {
                auto &textureView = faceTextures[layer.texture];
                if (!textureView) {
                    auto &image = *layer.texture;
                    D3D11_TEXTURE2D_DESC desc{};
                    desc.Width = image.width;
                    desc.Height = image.height;
                    desc.MipLevels = desc.ArraySize = 1;
                    desc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
                    desc.SampleDesc.Count = 1;
                    desc.Usage = D3D11_USAGE_IMMUTABLE;
                    desc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
                    D3D11_SUBRESOURCE_DATA data{image.rgba.data(), image.width * 4, 0};
                    ComPtr<ID3D11Texture2D> texture;
                    check(device->CreateTexture2D(&desc, &data, &texture),
                          tr8("error.graphics.face_layer_gpu_texture"));
                    check(device->CreateShaderResourceView(texture.Get(), nullptr, &textureView),
                          tr8("error.graphics.face_layer_gpu_view"));
                }
                context->PSSetShaderResources(0, 1, textureView.GetAddressOf());
                check(context->Map(vertices.Get(), 0, D3D11_MAP_WRITE_DISCARD, 0, &mapped),
                      tr8("error.graphics.map_face_layer"));
                memcpy(mapped.pData, layer.vertices.data(), sizeof(layer.vertices));
                context->Unmap(vertices.Get(), 0);
                context->Draw(6, 0);
            }
        }
    }
}
void Graphics::beginUi() {
    context->OMSetRenderTargets(0, nullptr, nullptr);
    canvas->BeginDraw();
    canvas->SetTransform(D2D1::Matrix3x2F::Identity());
}
bool Graphics::end() {
    HRESULT hr = canvas->EndDraw();
    if (hr == D2DERR_RECREATE_TARGET)
        return false;
    check(hr, tr8("error.graphics.direct2d_drawing"));
    if (!captureNextFrame.empty()) {
        ComPtr<ID3D11Texture2D> back, staging;
        check(swapChain->GetBuffer(0, IID_PPV_ARGS(&back)), tr8("error.graphics.capture_back_buffer"));
        D3D11_TEXTURE2D_DESC desc{};
        back->GetDesc(&desc);
        desc.Usage = D3D11_USAGE_STAGING;
        desc.BindFlags = 0;
        desc.CPUAccessFlags = D3D11_CPU_ACCESS_READ;
        desc.MiscFlags = 0;
        check(device->CreateTexture2D(&desc, nullptr, &staging),
              tr8("error.graphics.capture_staging_texture"));
        context->CopyResource(staging.Get(), back.Get());
        D3D11_MAPPED_SUBRESOURCE map{};
        check(context->Map(staging.Get(), 0, D3D11_MAP_READ, 0, &map), tr8("error.graphics.capture_pixels"));
        Image image{width, height, std::vector<uint8_t>(size_t(width) * height * 4)};
        for (unsigned y = 0; y < height; ++y) {
            auto row = static_cast<const uint8_t *>(map.pData) + y * map.RowPitch;
            for (unsigned x = 0; x < width; ++x) {
                auto dst = &image.rgba[(size_t(y) * width + x) * 4];
                dst[0] = row[x * 4 + 2];
                dst[1] = row[x * 4 + 1];
                dst[2] = row[x * 4];
                dst[3] = 255;
            }
        }
        context->Unmap(staging.Get(), 0);
        savePng(captureNextFrame, image);
        captureNextFrame.clear();
    }
    hr = swapChain->Present(1, 0);
    if (hr == DXGI_ERROR_DEVICE_REMOVED || hr == DXGI_ERROR_DEVICE_RESET)
        return false;
    check(hr, tr8("error.graphics.present"));
    return true;
}
void Graphics::fill(D2D1_RECT_F r, uint32_t c, float radius) {
    brush->SetColor(colorOf(c));
    if (radius)
        canvas->FillRoundedRectangle(D2D1::RoundedRect(r, radius, radius), brush.Get());
    else
        canvas->FillRectangle(r, brush.Get());
}
void Graphics::line(float x, float y, float x2, float y2, uint32_t c, float thickness) {
    brush->SetColor(colorOf(c));
    canvas->DrawLine({x, y}, {x2, y2}, brush.Get(), thickness);
}
void Graphics::contrastOutline(const Image &image, D2D1_RECT_F canvasBounds, D2D1_RECT_F bounds,
                               float thickness) {
    auto edge = [&](float x, float y, float x2, float y2) {
        int steps = std::max(1, int(std::ceil(std::max(std::abs(x2 - x), std::abs(y2 - y)) / 4)));
        for (int i = 0; i < steps; ++i) {
            float t0 = float(i) / steps, t1 = float(i + 1) / steps, tm = (t0 + t1) / 2;
            int px = std::clamp(int((x + (x2 - x) * tm - canvasBounds.left) * image.width /
                                    (canvasBounds.right - canvasBounds.left)),
                                0, int(image.width) - 1);
            int py = std::clamp(int((y + (y2 - y) * tm - canvasBounds.top) * image.height /
                                    (canvasBounds.bottom - canvasBounds.top)),
                                0, int(image.height) - 1);
            auto p = &image.rgba[(size_t(py) * image.width + px) * 4];
            bool light = (p[0] * 299 + p[1] * 587 + p[2] * 114) * p[3] / 255 > 128000;
            uint32_t ink = light ? 0x101010 : 0xFFFFFF, halo = light ? 0xFFFFFF : 0x101010;
            line(x + (x2 - x) * t0, y + (y2 - y) * t0, x + (x2 - x) * t1, y + (y2 - y) * t1, halo,
                 thickness + 2);
            line(x + (x2 - x) * t0, y + (y2 - y) * t0, x + (x2 - x) * t1, y + (y2 - y) * t1, ink, thickness);
        }
    };
    if (!image.width || !image.height)
        return;
    edge(bounds.left, bounds.top, bounds.right, bounds.top);
    edge(bounds.left, bounds.bottom, bounds.right, bounds.bottom);
    edge(bounds.left, bounds.top, bounds.left, bounds.bottom);
    edge(bounds.right, bounds.top, bounds.right, bounds.bottom);
}
void Graphics::text(const std::wstring &value, D2D1_RECT_F r, uint32_t c, int style) {
    brush->SetColor(colorOf(c));
    auto wrap = font->GetWordWrapping();
    if (style == 3)
        font->SetWordWrapping(DWRITE_WORD_WRAPPING_NO_WRAP);
    canvas->DrawTextW(value.data(), static_cast<UINT32>(value.size()),
                      style == 1   ? heading.Get()
                      : style == 2 ? mono.Get()
                                   : font.Get(),
                      r, brush.Get(), D2D1_DRAW_TEXT_OPTIONS_CLIP);
    if (style == 3)
        font->SetWordWrapping(wrap);
}
float Graphics::textWidth(const std::wstring &value) {
    ComPtr<IDWriteTextLayout> layout;
    check(textFactory->CreateTextLayout(value.data(), static_cast<UINT32>(value.size()), font.Get(), 10000,
                                        100, &layout),
          tr8("error.graphics.text_layout"));
    DWRITE_TEXT_METRICS metrics;
    check(layout->GetMetrics(&metrics), tr8("error.graphics.text_metrics"));
    return metrics.widthIncludingTrailingWhitespace;
}
size_t Graphics::textPosition(const std::wstring &value, float x) {
    ComPtr<IDWriteTextLayout> layout;
    check(textFactory->CreateTextLayout(value.data(), static_cast<UINT32>(value.size()), font.Get(), 10000,
                                        100, &layout),
          tr8("error.graphics.text_hit_layout"));
    BOOL trailing, inside;
    DWRITE_HIT_TEST_METRICS metrics{};
    check(layout->HitTestPoint(x, 5, &trailing, &inside, &metrics), tr8("error.graphics.text_hit_test"));
    return std::min(value.size(), size_t(metrics.textPosition + (trailing ? metrics.length : 0)));
}
void Graphics::image(const Image &img, D2D1_RECT_F r) {
    if (!img.width || !img.height)
        return;
    auto it = bitmaps.find(&img);
    if (it == bitmaps.end()) {
        std::vector<uint8_t> bgra = img.rgba;
        for (size_t i = 0; i < bgra.size(); i += 4) {
            auto a = bgra[i + 3];
            uint8_t red = bgra[i];
            bgra[i] = static_cast<uint8_t>(bgra[i + 2] * a / 255);
            bgra[i + 1] = static_cast<uint8_t>(bgra[i + 1] * a / 255);
            bgra[i + 2] = static_cast<uint8_t>(red * a / 255);
        }
        ComPtr<ID2D1Bitmap> bitmap;
        auto props = D2D1::BitmapProperties(
            D2D1::PixelFormat(DXGI_FORMAT_B8G8R8A8_UNORM, D2D1_ALPHA_MODE_PREMULTIPLIED), 96, 96);
        check(canvas->CreateBitmap({img.width, img.height}, bgra.data(), img.width * 4, props, &bitmap),
              tr8("error.graphics.portrait_bitmap"));
        it = bitmaps.emplace(&img, std::move(bitmap)).first;
    }
    canvas->DrawBitmap(it->second.Get(), r, 1, D2D1_BITMAP_INTERPOLATION_MODE_NEAREST_NEIGHBOR);
}
void Graphics::clip(D2D1_RECT_F r) {
    canvas->PushAxisAlignedClip(r, D2D1_ANTIALIAS_MODE_ALIASED);
}
void Graphics::unclip() {
    canvas->PopAxisAlignedClip();
}
} // namespace echo
