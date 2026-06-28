/**
 * Web Share API 服务
 * Validates: Requirements 27.5
 */

export interface ShareData {
  title: string
  text?: string
  url?: string
  files?: File[]
}

class WebShareService {
  isSupported(): boolean {
    return typeof navigator !== 'undefined' && !!navigator.share
  }

  canShareFiles(): boolean {
    return typeof navigator !== 'undefined' && !!navigator.canShare
  }

  async share(data: ShareData): Promise<boolean> {
    if (!this.isSupported()) return false
    try {
      await navigator.share(data)
      return true
    } catch {
      return false
    }
  }

  /**
   * 分享报表截图
   */
  async shareScreenshot(canvas: HTMLCanvasElement, title: string): Promise<boolean> {
    if (!this.canShareFiles()) return false

    return new Promise((resolve) => {
      canvas.toBlob(async (blob) => {
        if (!blob) { resolve(false); return }
        const file = new File([blob], `${title}.png`, { type: 'image/png' })
        const canShare = navigator.canShare?.({ files: [file] })
        if (!canShare) { resolve(false); return }
        try {
          await navigator.share({ title, files: [file] })
          resolve(true)
        } catch {
          resolve(false)
        }
      }, 'image/png')
    })
  }
}

export const webShareService = new WebShareService()
export default webShareService
