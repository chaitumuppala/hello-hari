import { NativeModules, NativeEventEmitter, Platform } from 'react-native';

const { CallDetector: CallDetectorModule } = NativeModules;

class CallDetectorManager {
  constructor() {
    this.eventEmitter = Platform.OS === 'android' ? new NativeEventEmitter(CallDetectorModule) : null;
    this.listeners = [];
  }

  async checkPermission() {
    if (Platform.OS !== 'android') return true;
    try {
      return await CallDetectorModule.checkPermission();
    } catch (error) {
      console.error('Error checking call detector permission:', error);
      return false;
    }
  }

  async startCallDetection(callback) {
    if (Platform.OS !== 'android') {
      console.warn('Call detection is only available on Android');
      return false;
    }

    try {
      if (this.eventEmitter && callback) {
        const listener = this.eventEmitter.addListener('CallStateChanged', callback);
        this.listeners.push(listener);
      }
      const result = await CallDetectorModule.startCallDetection();
      return result;
    } catch (error) {
      console.error('Error starting call detection:', error);
      throw error;
    }
  }

  async stopCallDetection() {
    if (Platform.OS !== 'android') return true;
    try {
      this.listeners.forEach(listener => {
        if (listener && typeof listener.remove === 'function') {
          listener.remove();
        }
      });
      this.listeners = [];
      const result = await CallDetectorModule.stopCallDetection();
      return result;
    } catch (error) {
      console.error('Error stopping call detection:', error);
      throw error;
    }
  }

  addListener(eventName, callback) {
    if (this.eventEmitter) {
      const listener = this.eventEmitter.addListener(eventName, callback);
      this.listeners.push(listener);
      return listener;
    }
    return null;
  }

  removeAllListeners() {
    this.listeners.forEach(listener => {
      if (listener && typeof listener.remove === 'function') {
        listener.remove();
      }
    });
    this.listeners = [];
  }
}

export const CallDetector = new CallDetectorManager();
