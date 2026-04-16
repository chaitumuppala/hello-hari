import { NativeModules, NativeEventEmitter, Platform } from 'react-native';

const { ScamAnalysis: ScamAnalysisModule } = NativeModules;

class ScamAnalysisManager {
  constructor() {
    this.eventEmitter = Platform.OS === 'android'
      ? new NativeEventEmitter(ScamAnalysisModule)
      : null;
    this.listeners = [];
  }

  /**
   * Set the backend URL (hello-hari-recorder server).
   * Defaults to the HuggingFace Space URL.
   */
  async setBackendUrl(url) {
    if (Platform.OS !== 'android') return;
    await ScamAnalysisModule.setBackendUrl(url);
  }

  /**
   * Start streaming audio to the backend for real-time scam analysis.
   * @param {string} language - ISO 639-1 language code (e.g. 'hi', 'te', 'en')
   */
  async startStreaming(language = 'hi') {
    if (Platform.OS !== 'android') {
      console.warn('Scam analysis streaming is only available on Android');
      return false;
    }

    try {
      const result = await ScamAnalysisModule.startStreaming(language);
      return result;
    } catch (error) {
      console.error('Error starting scam analysis stream:', error);
      throw error;
    }
  }

  /**
   * Stop the active streaming session.
   */
  async stopStreaming() {
    if (Platform.OS !== 'android') return null;
    try {
      const result = await ScamAnalysisModule.stopStreaming();
      return result;
    } catch (error) {
      console.error('Error stopping scam analysis stream:', error);
      throw error;
    }
  }

  /**
   * Check if currently streaming.
   */
  async isStreaming() {
    if (Platform.OS !== 'android') return false;
    try {
      return await ScamAnalysisModule.isStreaming();
    } catch (error) {
      return false;
    }
  }

  /**
   * Listen for real-time scam analysis results.
   * Callback receives: { type, text, language, riskScore, isScam, riskLevel, matchedPatterns, categoryScores }
   */
  onAnalysisResult(callback) {
    return this.addListener('scamAnalysisResult', (event) => {
      // Parse JSON strings from native side
      const result = { ...event };
      if (event.matchedPatterns) {
        try { result.matchedPatterns = JSON.parse(event.matchedPatterns); }
        catch (e) { result.matchedPatterns = []; }
      }
      if (event.categoryScores) {
        try { result.categoryScores = JSON.parse(event.categoryScores); }
        catch (e) { result.categoryScores = {}; }
      }
      callback(result);
    });
  }

  /**
   * Listen for stream status changes.
   * Callback receives: { status: 'connected'|'disconnected'|'error', error?: string }
   */
  onStreamStatus(callback) {
    return this.addListener('scamStreamStatus', callback);
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

export const ScamAnalysis = new ScamAnalysisManager();
