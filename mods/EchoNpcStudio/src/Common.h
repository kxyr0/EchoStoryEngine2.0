#pragma once
#include <windows.h>
#include <wrl/client.h>
#include <filesystem>
#include <string>
#include <vector>
#include <stdexcept>
#include <algorithm>
#include <cmath>
#include <cstdint>
#include "localization/Localization.h"
namespace echo {
inline int messageBox(HWND window, const std::wstring &message, const std::wstring &title, UINT flags) {
    return MessageBoxW(window, message.c_str(), title.c_str(), flags);
}
template <class T> using ComPtr = Microsoft::WRL::ComPtr<T>;
namespace fs = std::filesystem;
class DeviceLost : public std::runtime_error {
  public:
    DeviceLost() : std::runtime_error(tr8("error.common.graphics_device_lost")) {}
};
inline void check(HRESULT hr, const std::string &operation) {
    if (hr == static_cast<HRESULT>(0x887A0005) || hr == static_cast<HRESULT>(0x887A0007))
        throw DeviceLost();
    if (FAILED(hr))
        throw std::runtime_error(std::string(operation) + tr8("error.common.hresult") +
                                 std::to_string(static_cast<unsigned long>(hr)) + ")");
}
inline std::wstring wide(const std::string &text) {
    if (text.empty())
        return {};
    int n = MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, text.data(), static_cast<int>(text.size()),
                                nullptr, 0);
    if (!n)
        throw std::runtime_error(tr8("error.common.invalid_utf_8"));
    std::wstring out(n, L' ');
    MultiByteToWideChar(CP_UTF8, 0, text.data(), static_cast<int>(text.size()), out.data(), n);
    return out;
}
inline std::string utf8(const std::wstring &text) {
    if (text.empty())
        return {};
    int n = WideCharToMultiByte(CP_UTF8, WC_ERR_INVALID_CHARS, text.data(), static_cast<int>(text.size()),
                                nullptr, 0, nullptr, nullptr);
    if (!n)
        throw std::runtime_error(tr8("error.common.invalid_unicode_text"));
    std::string out(n, ' ');
    WideCharToMultiByte(CP_UTF8, 0, text.data(), static_cast<int>(text.size()), out.data(), n, nullptr,
                        nullptr);
    return out;
}
struct ComApartment {
    HRESULT result;
    explicit ComApartment(DWORD flags = COINIT_APARTMENTTHREADED) : result(CoInitializeEx(nullptr, flags)) {
        check(result, tr8("error.common.com_initialization"));
    }
    ~ComApartment() { CoUninitialize(); }
    ComApartment(const ComApartment &) = delete;
};
} // namespace echo
