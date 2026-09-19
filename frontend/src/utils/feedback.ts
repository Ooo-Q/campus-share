import { createDiscreteApi, lightTheme } from 'naive-ui'
import { morandiTheme } from '../theme/morandi'

const { message, dialog, notification } = createDiscreteApi(
  ['message', 'dialog', 'notification'],
  {
    configProviderProps: {
      theme: lightTheme,
      themeOverrides: morandiTheme,
    },
  },
)

export { message, dialog, notification }
