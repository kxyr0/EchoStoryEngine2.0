#pragma once
#include "graphics/MinecraftModel.h"
#include "skin/Skin.h"
#include <d3d11.h>
#include <dxgi1_2.h>
#include <d2d1_1.h>
#include <dwrite.h>
#include <unordered_map>
namespace echo {
class Graphics {
    HWND window = nullptr;
    unsigned width = 0, height = 0;
    float dpi = 96;
    ComPtr<ID3D11Device> device;
    ComPtr<ID3D11DeviceContext> context;
    ComPtr<IDXGISwapChain1> swapChain;
    ComPtr<ID3D11RenderTargetView> renderTarget;
    ComPtr<ID3D11DepthStencilView> depthTarget;
    ComPtr<ID2D1Factory1> factory;
    ComPtr<ID2D1Device> d2dDevice;
    ComPtr<ID2D1DeviceContext> canvas;
    ComPtr<ID2D1Bitmap1> surface;
    ComPtr<ID2D1SolidColorBrush> brush;
    ComPtr<IDWriteFactory> textFactory;
    ComPtr<IDWriteTextFormat> font, heading, mono;
    ComPtr<ID3D11VertexShader> vs;
    ComPtr<ID3D11PixelShader> ps;
    ComPtr<ID3D11InputLayout> inputLayout;
    ComPtr<ID3D11Buffer> vertices, constants;
    ComPtr<ID3D11SamplerState> sampler;
    ComPtr<ID3D11RasterizerState> raster;
    ComPtr<ID3D11BlendState> blend;
    ComPtr<ID3D11DepthStencilState> depthWrite, depthRead;
    ComPtr<ID3D11ShaderResourceView> skinTexture;
    std::shared_ptr<Skin> activeSkin;
    std::unordered_map<const Image *, ComPtr<ID3D11ShaderResourceView>> faceTextures;
    std::unordered_map<const Image *, ComPtr<ID2D1Bitmap>> bitmaps;
    void targets();
    void pipeline();

  public:
    bool warp = false;
    fs::path captureNextFrame;
    void initialize(HWND hwnd, bool forceWarp = false);
    void resize(unsigned w, unsigned h, float dpiValue);
    void setSkin(std::shared_ptr<Skin> skin);
    void invalidateImages() { bitmaps.clear(); }
    void begin();
    void model(D2D1_RECT_F bounds, const ModelSettings &settings, float yaw, float pitch, float distance,
               bool head, const FaceRig *faceRig = nullptr);
    void beginUi();
    bool end();
    void fill(D2D1_RECT_F r, uint32_t color, float radius = 0);
    void line(float x, float y, float x2, float y2, uint32_t color, float thickness = 1);
    void contrastOutline(const Image &image, D2D1_RECT_F canvasBounds, D2D1_RECT_F bounds,
                         float thickness = 2);
    void text(const std::wstring &value, D2D1_RECT_F r, uint32_t color = 0xE6E8EC, int style = 0);
    float textWidth(const std::wstring &value);
    size_t textPosition(const std::wstring &value, float x);
    void image(const Image &img, D2D1_RECT_F r);
    void clip(D2D1_RECT_F r);
    void unclip();
};
} // namespace echo
