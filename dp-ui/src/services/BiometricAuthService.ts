/**
 * 生物认证服务（指纹/面容解锁）
 * Uses Web Authentication API (WebAuthn)
 * Validates: Requirements 27.7
 */

export interface BiometricCredential {
  credentialId: string
  publicKey: string
  createdAt: number
}

class BiometricAuthService {
  isSupported(): boolean {
    return typeof window !== 'undefined' &&
      !!window.PublicKeyCredential &&
      typeof window.PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable === 'function'
  }

  async isPlatformAuthenticatorAvailable(): Promise<boolean> {
    if (!this.isSupported()) return false
    try {
      return await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable()
    } catch {
      return false
    }
  }

  /**
   * 注册生物认证凭据
   */
  async register(userId: string, userName: string): Promise<BiometricCredential | null> {
    if (!await this.isPlatformAuthenticatorAvailable()) return null

    try {
      const challenge = crypto.getRandomValues(new Uint8Array(32))
      const credential = await navigator.credentials.create({
        publicKey: {
          challenge,
          rp: { name: 'DataTeaCup', id: window.location.hostname },
          user: {
            id: new TextEncoder().encode(userId),
            name: userName,
            displayName: userName
          },
          pubKeyCredParams: [
            { alg: -7, type: 'public-key' },   // ES256
            { alg: -257, type: 'public-key' }   // RS256
          ],
          authenticatorSelection: {
            authenticatorAttachment: 'platform',
            userVerification: 'required'
          },
          timeout: 60000
        }
      }) as PublicKeyCredential | null

      if (!credential) return null

      const response = credential.response as AuthenticatorAttestationResponse
      return {
        credentialId: btoa(String.fromCharCode(...new Uint8Array(credential.rawId))),
        publicKey: btoa(String.fromCharCode(...new Uint8Array(response.getPublicKey()!))),
        createdAt: Date.now()
      }
    } catch {
      return null
    }
  }

  /**
   * 使用生物认证验证身份
   */
  async authenticate(credentialId: string): Promise<boolean> {
    if (!this.isSupported()) return false

    try {
      const challenge = crypto.getRandomValues(new Uint8Array(32))
      const rawId = Uint8Array.from(atob(credentialId), c => c.charCodeAt(0))

      const assertion = await navigator.credentials.get({
        publicKey: {
          challenge,
          allowCredentials: [{ id: rawId, type: 'public-key', transports: ['internal'] }],
          userVerification: 'required',
          timeout: 60000
        }
      })

      return assertion !== null
    } catch {
      return false
    }
  }
}

export const biometricAuthService = new BiometricAuthService()
export default biometricAuthService
