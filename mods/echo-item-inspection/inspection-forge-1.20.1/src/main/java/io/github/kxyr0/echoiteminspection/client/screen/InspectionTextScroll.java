package io.github.kxyr0.echoiteminspection.client.screen;

/** Line-based document scrolling, independent from the item's wheel zoom. */
public final class InspectionTextScroll {
    private int totalLines;
    private int visibleLines = 1;
    private int position;

    public void resize(int total, int visible) {
        totalLines = Math.max(0, total);
        visibleLines = Math.max(1, visible);
        position = Math.min(position, maximum());
    }

    public int position() { return position; }
    public int visibleLines() { return visibleLines; }
    public int maximum() { return Math.max(0, totalLines - visibleLines); }
    public boolean canScroll() { return maximum() > 0; }

    public void move(int lines) {
        position = (int) Math.max(0, Math.min(maximum(), (long) position + lines));
    }

    public void page(int direction) {
        move(direction * Math.max(1, visibleLines - 1));
    }

    public void seek(double fraction) {
        if (!Double.isFinite(fraction)) return;
        position = (int) Math.round(Math.max(0, Math.min(1, fraction)) * maximum());
    }

    public double fraction() {
        return maximum() == 0 ? 0 : position / (double) maximum();
    }

    public double visibleFraction() {
        return totalLines == 0 ? 1 : Math.min(1, visibleLines / (double) totalLines);
    }
}
