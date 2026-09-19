import type { GlobalThemeOverrides } from 'naive-ui'

/** Monochromatic Morandi sage — approachable luxury / glassmorphism */
export const morandiTheme: GlobalThemeOverrides = {
  common: {
    primaryColor: '#7A9E8E',
    primaryColorHover: '#6B8F7F',
    primaryColorPressed: '#5C8070',
    primaryColorSuppl: '#8FAF9F',
    infoColor: '#8BA3A8',
    successColor: '#8A9E7E',
    warningColor: '#C4A484',
    errorColor: '#C4897E',
    textColorBase: '#3D4540',
    textColor1: '#3D4540',
    textColor2: '#6B756F',
    textColor3: '#9AA39C',
    borderRadius: '16px',
    borderRadiusSmall: '12px',
    fontFamily:
      '"Outfit", "HarmonyOS Sans", "PingFang SC", "Microsoft YaHei", system-ui, sans-serif',
    fontSize: '15px',
    heightMedium: '40px',
    heightLarge: '48px',
  },
  Button: {
    borderRadiusMedium: '999px',
    borderRadiusLarge: '999px',
    borderRadiusSmall: '999px',
    heightMedium: '40px',
    heightLarge: '48px',
    fontWeight: '560',
  },
  Input: {
    borderRadius: '16px',
    heightMedium: '44px',
    heightLarge: '48px',
  },
  Card: {
    borderRadius: '24px',
    paddingMedium: '24px',
  },
  Dialog: {
    borderRadius: '28px',
    padding: '28px',
  },
  Modal: {
    borderRadius: '28px',
  },
  Tag: {
    borderRadius: '999px',
  },
  DataTable: {
    borderRadius: '20px',
  },
  Menu: {
    borderRadius: '16px',
    itemHeight: '44px',
  },
  Tabs: {
    tabBorderRadius: '999px',
  },
}
