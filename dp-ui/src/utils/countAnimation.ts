import { ref, watch, type Ref } from 'vue'

/**
 * Ease-out cubic easing function: decelerating to zero velocity.
 * t is the progress from 0 to 1.
 * This function is monotonically increasing on [0, 1].
 */
function easeOut(t: number): number {
  return 1 - Math.pow(1 - t, 3)
}

/**
 * Composable that animates a number from 0 to a target value.
 * The animation always starts from 0 and monotonically increases to the target.
 *
 * @param target - A ref containing the target number to animate to
 * @param duration - Animation duration in milliseconds (default: 800)
 * @returns A ref containing the current animated value
 */
export function useCountAnimation(target: Ref<number>, duration = 800): Ref<number> {
  const current = ref(0)
  let animationFrameId: number | null = null

  function animate(targetValue: number) {
    // Cancel any running animation
    if (animationFrameId !== null) {
      cancelAnimationFrame(animationFrameId)
      animationFrameId = null
    }

    // Clamp target to non-negative to ensure monotonic increase from 0
    const clampedTarget = Math.max(0, targetValue)

    // Always start from 0 per requirement 2.6
    current.value = 0

    // No animation needed for zero target or zero duration
    if (clampedTarget === 0 || duration <= 0) {
      current.value = clampedTarget
      return
    }

    const startTime = performance.now()
    let lastValue = 0

    function step(now: number) {
      const elapsed = now - startTime
      const progress = Math.min(elapsed / duration, 1)
      const easedProgress = easeOut(progress)

      // Calculate the raw value and use floor to avoid overshooting,
      // then enforce monotonic increase by taking max with last value
      const rawValue = Math.round(easedProgress * clampedTarget)
      const nextValue = Math.max(lastValue, Math.min(rawValue, clampedTarget))

      current.value = nextValue
      lastValue = nextValue

      if (progress < 1) {
        animationFrameId = requestAnimationFrame(step)
      } else {
        // Ensure we land exactly on the target value
        current.value = clampedTarget
        animationFrameId = null
      }
    }

    animationFrameId = requestAnimationFrame(step)
  }

  watch(target, (newVal) => {
    animate(newVal)
  }, { immediate: true })

  return current
}
