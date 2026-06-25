/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<object, object, unknown>
  export default component
}

declare module '@vicons/ionicons5' {
  import type { Component } from 'vue'
  export const BulbOutline: Component
  export const ChatbubbleOutline: Component
  export const CalendarOutline: Component
  export const LayersOutline: Component
  export const PencilOutline: Component
  export const RefreshOutline: Component
  export const BarChartOutline: Component
  export const ServerOutline: Component
  export const PeopleOutline: Component
  export const SettingsOutline: Component
  export const DocumentTextOutline: Component
  export const CloudUploadOutline: Component
  export const CogOutline: Component
  export const LogOutOutline: Component
  export const MenuOutline: Component
  export const CloseOutline: Component
  export const AddOutline: Component
  export const SearchOutline: Component
  export const TrashOutline: Component
  export const CheckmarkOutline: Component
  export const AlertCircleOutline: Component
  export const SunOutline: Component
  export const MoonOutline: Component
  export const TimeOutline: Component
  export const StarOutline: Component
  export const TrendingUpOutline: Component
  export const FlashOutline: Component
  export const CopyOutline: Component
  export const DownloadOutline: Component
  export const EyeOutline: Component
  export const EyeOffOutline: Component
  export const KeyOutline: Component
  export const ShieldCheckmarkOutline: Component
  export const ClipboardOutline: Component
  export const FileTrayFullOutline: Component
  export const GitBranchOutline: Component
}