export function useHaptic() {
  const vibrate = (pattern: number | number[] = 10) => {
    if (navigator.vibrate) {
      navigator.vibrate(pattern);
    }
  };

  const lightTap = () => vibrate(8);
  const mediumTap = () => vibrate(15);
  const heavyTap = () => vibrate([10, 30, 10]);

  return { vibrate, lightTap, mediumTap, heavyTap };
}
