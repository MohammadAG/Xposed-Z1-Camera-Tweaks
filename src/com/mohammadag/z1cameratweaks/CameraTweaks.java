package com.mohammadag.z1cameratweaks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;

import java.lang.reflect.Constructor;

import android.view.KeyEvent;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class CameraTweaks implements IXposedHookLoadPackage {

	protected Object mStateMachine;
	private static XSharedPreferences mPreferences;

	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (!lpparam.packageName.equals(Constants.PACKAGE_NAME))
			return;

		mPreferences = new XSharedPreferences(Constants.MOD_PACKAGE_NAME);

		Class<?> EventDispatcher = findClass(Constants.PACKAGE_NAME + ".controller.EventDispatcher",
				lpparam.classLoader);
		Class<?> ControllerEventSource = findClass(Constants.PACKAGE_NAME + ".controller.ControllerEventSource",
				lpparam.classLoader);
		Class<?> EventAction = findClass(Constants.PACKAGE_NAME + ".controller.EventAction",
				lpparam.classLoader);

		findAndHookMethod("com.sonyericsson.android.camera.ExtendedActivity", lpparam.classLoader,
				"onResume", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				mPreferences.reload();
			}
		});

		try {
			/* This was a surprise to me, the camera started by the hardware button
			 * is a whole another activity that's different from the one opened when
			 * using the camera icon.
			 */
			Class<?> StateMachine = findClass(Constants.PACKAGE_NAME + ".fastcapturing.StateMachine", lpparam.classLoader);
			Class<?> StatePhotoCapture = findClass(Constants.PACKAGE_NAME + ".fastcapturing.StateMachine$StatePhotoCapture",
					lpparam.classLoader);
			final Constructor<?> StatePhotoCaptureConstructor = StatePhotoCapture.getConstructor(StateMachine);

			XposedBridge.hookAllConstructors(StateMachine, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					mStateMachine = param.thisObject;
				}
			});

			findAndHookMethod(Constants.PACKAGE_NAME + ".fastcapturing.StateMachine$StatePhotoAfDone", lpparam.classLoader,
					"handleKeyCaptureDown", Object[].class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if (mStateMachine != null && mPreferences.getBoolean(Constants.KEY_DISABLE_HW_BURST, false)) {
						XposedHelpers.callMethod(mStateMachine, "doCapture");
						XposedHelpers.callMethod(mStateMachine, "changeTo",
								StatePhotoCaptureConstructor.newInstance(mStateMachine), param.args[0]);
						param.setResult(null);
					}
				}
			});
		} catch (Exception e) {
			XposedBridge.log("Failed to hook fast capture");
			e.printStackTrace();
		}

		final Object keyType = getStaticObjectField(ControllerEventSource, "KEY");
		final Object upAction = getStaticObjectField(EventAction, "UP");
		findAndHookMethod(EventDispatcher, "sendLongPressEvent", ControllerEventSource, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!mPreferences.getBoolean(Constants.KEY_DISABLE_HW_BURST, false))
					return;

				Object source = param.args[0];

				if (source == keyType)
					param.setResult(null);
			}
		});

		findAndHookMethod(EventDispatcher, "onKeyDown", int.class, KeyEvent.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!mPreferences.getBoolean(Constants.KEY_DISABLE_HW_BURST, false))
					return;

				int keyCode = (Integer) param.args[0];
				if (keyCode == KeyEvent.KEYCODE_CAMERA) {
					XposedHelpers.callMethod(param.thisObject, "sendCaptureEvent", upAction, keyType);
				}
			}
		});

		findAndHookMethod(EventDispatcher, "onKeyUp", int.class, KeyEvent.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (!mPreferences.getBoolean(Constants.KEY_DISABLE_HW_BURST, false))
					return;

				int keyCode = (Integer) param.args[0];
				if (keyCode == KeyEvent.KEYCODE_CAMERA) {
					param.setResult(false);
				}
			}
		});

		findAndHookMethod("com.sonyericsson.android.camera.fastcapturing.FastCapturingActivity",
				lpparam.classLoader, "playStartUpNotificationSoundIfRequired", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if (mPreferences.getBoolean(Constants.KEY_DISABLE_FASTCAPTURE_SOUND, false))
					param.setResult(null);
			}
		});
	}
}
