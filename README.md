# 通话震动 #

[![Build Status](https://travis-ci.org/shaobin0604/CallVibrator.png?branch=master)](https://travis-ci.org/shaobin0604/CallVibrator)

Android平台最好的通话震动辅助软件。

如果您觉得这个软件还不错，并愿意提供一些资金支持我继续开发的话，我表示衷心的感谢。

<a href='http://me.alipay.com/shaobin0604'><img src='https://img.alipay.com/sys/personalprod/style/mc/btn-index.png' /></a>

## 功能简介 ##

*   拔出电话接通时（对方按下接听键）震动提醒。
*   呼入电话接通时震动提醒。
*   电话挂断时震动提醒。
*   呼出通话时间震动提醒。
*   支持震动时长设置。

## 支持的机型 ##

由于 *Android SDK* 没有提供呼出通话 *对方震铃* 到 *对方摘机* 状态监听的API，本软件对拔出电话接通时（对方按下接听键）震动提醒是通过读取系统的 *radio log* 实现的。如果您的设备/ROM不输出 *radio log* 或是输出的 *radio log* 和标准 *AOSP* 版本不一样，那么本软件的接通震动功能在您的设备上无法正常工作。另外，由于没有电信版 *CDMA* 手机供开发，本软件暂不支持 *CDMA* 手机。 

以下是目前测试有效的设备/ROM。

*   HTC G1 Dream(rooted)/CM 5.0.8
*   Google Nexus S(rooted) 4.1/4.2
*   Google Galaxy Nexus(rooted) 4.1/4.2/4.3
*   Google Nexus 4(rooted) 4.2/4.3
*   Google Nexus 5 4.4.2
*   Samsung 7680(rooted)/ophone 2.0
*   Samsung Galaxy III 4.4.2
*   Motorola Defy(rooted)/MIUI 2.3.7c/CM 10,11
*   HuaWei T8301 2.2
*   K-Touch W606 2.1   
*   HuaWei C8650 2.2 **CDMA** work
*   HTC S710d 4.0.3 **CDMA** work
*   HTC X720d 4.0.3 **CDMA** work, **GSM** not work

由于 Android 设备/ROM 众多，如果本软件在您的手机上可以正常工作，还请提供您的设备型号/ROM信息给我，我把它加入到本软件的支持设备列表中，以便新用户参考。如果本软件在您的手机上不能正常工作，还请及时反馈，联系方式：shaobin0604@qq.com。

## 关于权限 ##

*   `android.permission.PROCESS_OUTGOING_CALLS` 获取用户是否正在拨出电话
*   `android.permission.READ_PHONE_STATE` 监听电话状态变化（振铃，通话中，挂断）
*   `android.permission.READ_LOGS` 读取系统 *radio log* , 实现呼出接通震动
*   `android.permission.VIBRATE` 实现震动功能
*   `android.permission.WRITE_EXTERNAL_STORAGE` 保存日志到SD卡
*   `android.permission.WAKE_LOCK` 确保手机在通话过程中不休眠
