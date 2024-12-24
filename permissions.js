import { NativeModules, Platform } from 'react-native';

const { PermissionModule } = NativeModules;

export const Permissions = {
  async checkPermissions() {
    if (Platform.OS !== 'android') return { allGranted: true };
    try {
      return await PermissionModule.checkPermissions();
    } catch (error) {
      console.error('Error checking permissions:', error);
      return { allGranted: false };
    }
  },

  async requestPermissions() {
    if (Platform.OS !== 'android') return true;
    try {
      return await PermissionModule.requestPermissions();
    } catch (error) {
      console.error('Error requesting permissions:', error);
      return false;
    }
  },

  async openSettings() {
    if (Platform.OS !== 'android') return;
    try {
      await PermissionModule.openSettings();
    } catch (error) {
      console.error('Error opening settings:', error);
    }
  }
};
