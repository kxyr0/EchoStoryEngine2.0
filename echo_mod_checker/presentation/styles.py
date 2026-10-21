from __future__ import annotations

COLORS = {
    "bg": "#15171B",
    "surface": "#1C1F25",
    "raised": "#22262D",
    "border": "#2D323B",
    "text": "#ECEEF2",
    "muted": "#A1A7B0",
    "disabled": "#737A84",
    "accent": "#7892E8",
    "accent_hover": "#879FED",
    "success": "#67B88B",
    "warning": "#D2A453",
    "error": "#D36F75",
    "info": "#739FC7",
    "duplicate": "#A889C8",
}


def app_stylesheet() -> str:
    c = COLORS
    return f"""
    QWidget#Root {{
        background-color: {c["bg"]};
    }}
    QWidget {{
        color: {c["text"]};
        font-family: "Segoe UI";
        font-size: 14px;
        letter-spacing: 0;
    }}
    QLabel {{
        background: transparent;
        border: none;
    }}
    QFrame#Header, QFrame#StatusCard, QFrame#ProblemsPanel, QFrame#DetailsPanel, QFrame#Footer, QDialog {{
        background-color: {c["surface"]};
        border: 1px solid {c["border"]};
        border-radius: 10px;
    }}
    QLabel#title {{ font-size: 15px; font-weight: 600; }}
    QLabel#state {{ font-size: 28px; font-weight: 650; }}
    QLabel#subtitle, QLabel#muted, QLabel#path {{ color: {c["muted"]}; }}
    QPushButton {{
        background: {c["raised"]};
        color: {c["text"]};
        border: 1px solid {c["border"]};
        border-radius: 9px;
        padding: 8px 14px;
    }}
    QPushButton:hover {{ border-color: {c["accent"]}; }}
    QPushButton#primary {{
        background: {c["accent"]};
        color: {c["text"]};
        border: none;
        border-radius: 10px;
        padding: 0 28px;
        font-weight: 600;
        text-align: center;
        min-width: 280px;
        min-height: 44px;
        max-height: 44px;
    }}
    QPushButton#primary:hover {{ background: {c["accent_hover"]}; }}
    QPushButton#primary:disabled {{ background: {c["border"]}; color: {c["disabled"]}; }}
    QPushButton#secondary {{
        color: {c["success"]};
        border-color: {c["success"]};
        font-weight: 600;
    }}
    QPushButton#icon {{
        background: {c["raised"]};
        border: 1px solid {c["border"]};
        border-radius: 9px;
        padding: 0;
        min-width: 64px;
        min-height: 52px;
        max-width: 64px;
        max-height: 52px;
    }}
    QPushButton#icon:hover {{ border-color: {c["accent"]}; }}
    QProgressBar {{
        background: {c["raised"]};
        border: 1px solid {c["border"]};
        border-radius: 6px;
        height: 8px;
        text-align: center;
    }}
    QProgressBar::chunk {{ background: {c["accent"]}; border-radius: 5px; }}
    QAbstractScrollArea, QListWidget, QListWidget::viewport {{
        background-color: transparent;
        border: none;
        color: {c["text"]};
    }}
    QListWidget {{
        padding: 6px;
        outline: none;
    }}
    QListWidget::item {{ border-radius: 8px; color: {c["text"]}; padding: 8px; }}
    QListWidget::item:selected {{ background: {c["raised"]}; color: {c["text"]}; }}
    QScrollBar:vertical {{
        background: transparent;
        width: 9px;
        margin: 0;
    }}
    QScrollBar::handle:vertical {{
        background: #3A404A;
        border-radius: 4px;
        min-height: 28px;
    }}
    QScrollBar::handle:vertical:hover {{ background: #4B5360; }}
    QScrollBar::add-line:vertical, QScrollBar::sub-line:vertical {{
        height: 0;
        background: transparent;
        border: none;
    }}
    QScrollBar::add-page:vertical, QScrollBar::sub-page:vertical {{ background: transparent; }}
    QLineEdit, QComboBox {{
        background: {c["raised"]};
        border: 1px solid {c["border"]};
        border-radius: 8px;
        padding: 8px 10px;
    }}
    """


def status_color(kind: str) -> str:
    return COLORS.get(kind, COLORS["muted"])
