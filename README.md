# MP3 Launcher

复古 MP3 播放器风格的 Android 启动器。项目目标是在桌面里保留音乐播放器的核心体验，用模拟机身、像素 LCD、实体按键和应用栏组成一个可日常使用的极简 Launcher。

![fig1](/tempshow/fig1.jpg)
![fig2](/tempshow/fig2.jpg)

## 当前功能

### 音乐主屏

- 显示当前媒体标题、作者、播放状态、进度、音量和电量。
- 专辑封面会做像素化和轻微褪色处理，贴近复古 LCD 屏幕效果。
- 支持通知监听读取系统媒体通知，需要开启“通知访问”权限。
- 播放、暂停、上一首、下一首、快退、快进、音量加减都有模拟实体按键。
- 长按播放/暂停键可打开设置里的默认音乐 App。
- 可选“模拟屏幕横滑切歌”：在 LCD 屏幕左滑下一首，右滑上一首，并带有短促 LCD 滑动动画。

### 复古界面

- 支持多套主题皮肤：黑绿 LCD、银色 Discman、透明 iPod、Game Boy、Walkman。
- 像素磁带播放机开机动画，当前播放速度较快，避免进入桌面时停留太久。
- 机械 HOLD 拨杆用于进入极简模式：隐藏应用栏，但不改变播放器主体布局。
- 音频输出指示灯使用复古彩灯：
  - 红色：扬声器
  - 蓝色：蓝牙
  - 绿色：有线耳机

### Launcher 能力

- 顶部应用栏显示桌面应用和设置入口。
- 默认标签页为 `ALL` 和 `RECENT`，自定义标签可从设置页添加。
- 支持隐藏应用、置顶应用、最近使用应用。
- 长按应用图标弹出操作菜单：
  - 置顶/取消置顶
  - 隐藏
  - 应用详情
  - 卸载
- 设置页可管理置顶应用、隐藏应用和自定义标签分组。
- 横屏下应用栏改为纵向滚动，标签切换仍使用横滑手势。

### 按键交互

- 启动器前台时，实体音量键可配置：
  - 切歌 + 播放
  - 音量 + 播放
  - 系统默认
- 默认逻辑：
  - 音量上短按：上一首
  - 音量下短按：下一首
  - 长按音量键：播放/暂停
- 媒体控制逻辑统一在 `MediaCommandDispatcher` 中，主界面按钮、LCD 横滑和音量键会复用同一套媒体命令。

## 设置页

设置页已支持中文显示，主要包含：

- 通知访问权限引导。
- 默认桌面设置入口。
- 未播放时默认音乐 App。
- 实体音量键模式。
- 锁屏式极简模式。
- 开机动画开关。
- 模拟屏幕横滑切歌开关。
- 主题皮肤切换。
- 置顶/隐藏应用管理。
- 自定义应用标签分组管理。

## 当前限制

- 锁屏时实体音量键快捷切歌暂不启用。
- 曾尝试通过 Android 无障碍服务实现锁屏快捷键，但在三星设备上会触发 SystemUI 锁屏/通知层覆盖，导致设置页看似卡住、无法点击。该无障碍服务注册和设置入口已撤回。
- Android 普通 Launcher Activity 无法在真正锁屏/息屏时稳定接收实体音量键事件。后续如继续实现，需要改用更谨慎的方案，并优先保证不影响系统输入和锁屏层。

## 权限

### 通知访问

用于读取和控制当前媒体通知。没有通知访问时，播放器信息和媒体控制可能无法同步。

入口：

```text
设置 -> 系统 -> 通知访问
```

### 默认桌面

用于让 MP3 Launcher 成为系统 Home 应用。

入口：

```text
设置 -> 系统 -> 默认桌面设置
```

## 构建与安装

项目使用 Android Gradle Plugin 和 Kotlin。

编译 Debug 包：

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' bash ./gradlew :app:assembleDebug
```

运行单元测试：

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' bash ./gradlew :app:testDebugUnitTest
```

安装到当前连接设备：

```bash
/Users/chaohsuan/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

指定设备安装示例：

```bash
/Users/chaohsuan/Library/Android/sdk/platform-tools/adb -s R5CTA1XVEFZ install -r app/build/outputs/apk/debug/app-debug.apk
```

## 开发备注

- 屏幕老化效果已移除，不再保留相关代码。
- 应用栏标签不常显，只在切换或滚动提示时显示。
- 默认导航标签固定使用英文 `ALL` 和 `RECENT`。
- 新功能应尽量保持“除了顶部应用栏，主体均为复古拟物播放器”的方向。
