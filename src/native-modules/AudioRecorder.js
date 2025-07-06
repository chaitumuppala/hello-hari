import { NativeModules, NativeEventEmitter, Platform } from 'react-native';

const { AudioRecorder: AudioRecorderModule } = NativeModules;

class AudioRecorderManager {
  constructor() {
    this.eventEmitter = Platform.OS === 'android' ? new NativeEventEmitter(AudioRecorderModule) : null;
    this.listeners = [];
  }

  async start() {
    if (Platform.OS !== 'android') {
      console.warn('Audio recording is only available on Android');
      return false;
    }

    try {
      const result = await AudioRecorderModule.start();
      return result;
    } catch (error) {
      console.error('Error starting audio recording:', error);
      throw error;
    }
  }

  async stop() {
    if (Platform.OS !== 'android') return null;
    try {
      const filePath = await AudioRecorderModule.stop();
      return filePath;
    } catch (error) {
      console.error('Error stopping audio recording:', error);
      throw error;
    }
  }

  async isRecording() {
    if (Platform.OS !== 'android') return false;
    try {
      const recording = await AudioRecorderModule.isRecording();
      return recording;
    } catch (error) {
      console.error('Error checking recording status:', error);
      return false;
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

export const AudioRecorder = new AudioRecorderManager();
