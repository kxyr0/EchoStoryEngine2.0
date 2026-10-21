const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
const coarse = window.matchMedia("(pointer: coarse)").matches;

document.documentElement.dataset.motion = reduced ? "reduced" : "full";

if (!reduced && !coarse) {
  import("lenis").then(({ default: Lenis }) => {
    const lenis = new Lenis({ duration: 0.9, smoothWheel: true });
    const frame = (time: number) => {
      lenis.raf(time);
      requestAnimationFrame(frame);
    };
    requestAnimationFrame(frame);
  });
}
