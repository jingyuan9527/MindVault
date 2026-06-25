import { config } from '@vue/test-utils'

config.global.stubs['router-link'] = {
  template: '<a><slot /></a>'
}

const NAIVE_COMPONENTS = [
  'NCard', 'NButton', 'NInput', 'NSelect', 'NForm', 'NFormItem',
  'NModal', 'NTag', 'NSpace', 'NLayout', 'NLayoutSider', 'NLayoutHeader',
  'NLayoutContent', 'NMenu', 'NMenuItem', 'NMessageProvider',
  'NConfigProvider', 'NSpin', 'NEmpty', 'NPagination', 'NDataTable',
  'NTime', 'NBadge', 'NIcon', 'NList', 'NListItem', 'NRadioGroup',
  'NRadio', 'NRadioButton', 'NGrid', 'NGi', 'NStatistic', 'NAlert', 'NSkeleton',
  'NScrollbar', 'NCheckbox', 'NSwitch', 'NDynamicTags', 'NStep', 'NSteps',
  'NTabs', 'NTabPane', 'NUpload', 'NPopover', 'NTooltip', 'NDrawer', 'NDrawerContent',
  'NResult', 'NCode', 'NThing', 'NText', 'NDescriptions', 'NDescriptionsItem',
  'NDatePicker', 'NInputNumber', 'NButtonGroup', 'NDivider', 'NA'
]

for (const name of NAIVE_COMPONENTS) {
  config.global.stubs[name] = {
    props: {
      // Accept common props
      title: String,
      size: String,
      type: String,
      loading: Boolean,
      disabled: Boolean,
      bordered: Boolean,
      hoverable: Boolean,
      closable: Boolean,
      value: [String, Number],
      modelValue: [String, Number, Array, Boolean],
      show: Boolean,
      preset: String,
      src: String,
      description: String,
      label: String,
      // DataTable
      columns: Array,
      data: Array,
      pagination: [Boolean, Object],
      'single-line': Boolean,
      // NTime
      time: [Number, Date, String],
      format: String,
      // NBadge
      max: [Number, String],
      // NModal
      'mask-closable': Boolean,
    },
    template: '<div><slot /><slot name="header" /><slot name="footer" /><slot name="default" /><slot name="extra" /><slot name="action" /><slot name="trigger" /><slot name="prefix" /><slot name="suffix" /><slot name="#" /></div>'
  }
}

// Mock ResizeObserver
global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}

// Naive UI auto-imported hooks (unplugin-auto-import not active in tests)
import { vi } from 'vitest'
globalThis.useDialog = () => ({ warning: vi.fn(), info: vi.fn(), success: vi.fn(), error: vi.fn() })
globalThis.useMessage = () => ({ success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() })
globalThis.useNotification = () => ({})


