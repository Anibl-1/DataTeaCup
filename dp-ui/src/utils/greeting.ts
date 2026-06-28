/**
 * 根据小时值返回时段问候语
 * Property 25: 0-5=夜深了, 6-11=早上好, 12-17=下午好, 18-23=晚上好
 */
export function getGreetingByHour(hour: number): string {
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
}
