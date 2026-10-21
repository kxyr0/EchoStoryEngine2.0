#include "app/Application.h"
#include <shellapi.h>
int WINAPI wWinMain(HINSTANCE instance, HINSTANCE, PWSTR, int show) {
    try {
        int count = 0;
        auto argv = CommandLineToArgvW(GetCommandLineW(), &count);
        std::vector<std::wstring> args;
        if (argv) {
            for (int i = 1; i < count; ++i)
                args.emplace_back(argv[i]);
            LocalFree(argv);
        }
        wchar_t executable[32768]{};
        GetModuleFileNameW(nullptr, executable, DWORD(std::size(executable)));
        std::wstring language;
        for (size_t i = 0; i + 1 < args.size(); ++i)
            if (args[i] == L"--lang")
                language = args[++i];
        auto warnings =
            echo::loadLocalization(echo::fs::path(executable).parent_path() / L"locales", language);
        echo::ComApartment apartment;
        echo::Application app;
        return app.run(instance, show, args, warnings);
    } catch (const std::exception &e) {
        echo::messageBox(nullptr, echo::wide(e.what()).c_str(),
                         echo::tr("ui.main.echo_npc_studio_startup_error"), MB_OK | MB_ICONERROR);
        return 1;
    }
}
