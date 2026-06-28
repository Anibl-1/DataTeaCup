/**
 * 头像预设数据
 * 提供预设头像供用户选择，每个头像包含唯一标识、渐变背景色和显示字符
 */

export interface AvatarPreset {
  id: string
  gradient: string
  icon: string
}

export const avatarPresets: AvatarPreset[] = [
  // 人物表情
  { id: 'avatar-1', gradient: 'linear-gradient(135deg, #667eea, #764ba2)', icon: '😊' },
  { id: 'avatar-2', gradient: 'linear-gradient(135deg, #f093fb, #f5576c)', icon: '😎' },
  { id: 'avatar-3', gradient: 'linear-gradient(135deg, #4facfe, #00f2fe)', icon: '🧑‍💻' },
  { id: 'avatar-4', gradient: 'linear-gradient(135deg, #43e97b, #38f9d7)', icon: '🤖' },
  { id: 'avatar-17', gradient: 'linear-gradient(135deg, #ff758c, #ff7eb3)', icon: '😄' },
  { id: 'avatar-18', gradient: 'linear-gradient(135deg, #7f7fd5, #86a8e7)', icon: '🤓' },
  { id: 'avatar-19', gradient: 'linear-gradient(135deg, #f7971e, #ffd200)', icon: '😏' },
  { id: 'avatar-20', gradient: 'linear-gradient(135deg, #00c6fb, #005bea)', icon: '🥳' },
  // 动物
  { id: 'avatar-5', gradient: 'linear-gradient(135deg, #fa709a, #fee140)', icon: '🐱' },
  { id: 'avatar-6', gradient: 'linear-gradient(135deg, #a18cd1, #fbc2eb)', icon: '🐶' },
  { id: 'avatar-7', gradient: 'linear-gradient(135deg, #fccb90, #d57eeb)', icon: '🦊' },
  { id: 'avatar-8', gradient: 'linear-gradient(135deg, #e0c3fc, #8ec5fc)', icon: '🐼' },
  { id: 'avatar-21', gradient: 'linear-gradient(135deg, #fbc2eb, #a6c1ee)', icon: '🐰' },
  { id: 'avatar-22', gradient: 'linear-gradient(135deg, #fdcbf1, #e6dee9)', icon: '🦁' },
  { id: 'avatar-23', gradient: 'linear-gradient(135deg, #a1c4fd, #c2e9fb)', icon: '🐧' },
  { id: 'avatar-24', gradient: 'linear-gradient(135deg, #d4fc79, #96e6a1)', icon: '🦋' },
  // 自然 & 物品
  { id: 'avatar-9', gradient: 'linear-gradient(135deg, #f5576c, #ff6a00)', icon: '🔥' },
  { id: 'avatar-10', gradient: 'linear-gradient(135deg, #0acffe, #495aff)', icon: '💎' },
  { id: 'avatar-11', gradient: 'linear-gradient(135deg, #69ff97, #00e4ff)', icon: '🌿' },
  { id: 'avatar-12', gradient: 'linear-gradient(135deg, #ff9a9e, #fad0c4)', icon: '🌸' },
  { id: 'avatar-13', gradient: 'linear-gradient(135deg, #ffecd2, #fcb69f)', icon: '⭐' },
  { id: 'avatar-14', gradient: 'linear-gradient(135deg, #a1c4fd, #c2e9fb)', icon: '🌊' },
  { id: 'avatar-15', gradient: 'linear-gradient(135deg, #d4fc79, #96e6a1)', icon: '🍀' },
  { id: 'avatar-16', gradient: 'linear-gradient(135deg, #84fab0, #8fd3f4)', icon: '🚀' },
  { id: 'avatar-25', gradient: 'linear-gradient(135deg, #ffd89b, #19547b)', icon: '🌅' },
  { id: 'avatar-26', gradient: 'linear-gradient(135deg, #c471f5, #fa71cd)', icon: '🎵' },
  { id: 'avatar-27', gradient: 'linear-gradient(135deg, #48c6ef, #6f86d6)', icon: '🎮' },
  { id: 'avatar-28', gradient: 'linear-gradient(135deg, #feada6, #f5efef)', icon: '🎨' },
  // 食物 & 运动
  { id: 'avatar-29', gradient: 'linear-gradient(135deg, #f6d365, #fda085)', icon: '🍕' },
  { id: 'avatar-30', gradient: 'linear-gradient(135deg, #fbc2eb, #a18cd1)', icon: '🧁' },
  { id: 'avatar-31', gradient: 'linear-gradient(135deg, #89f7fe, #66a6ff)', icon: '⚽' },
  { id: 'avatar-32', gradient: 'linear-gradient(135deg, #fddb92, #d1fdff)', icon: '🏀' },
]
