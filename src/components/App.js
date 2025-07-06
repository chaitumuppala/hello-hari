import React, { useState, useEffect } from 'react';
import { 
  SafeAreaView,
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  Modal,
  Platform
} from 'react-native';
import { CallDetector } from '../native-modules/CallDetector';
import { AudioRecorder } from '../native-modules/AudioRecorder';
import { Permissions } from '../../permissions';

const App = () => {
  const [permissionsGranted, setPermissionsGranted] = useState(false);
  const [isMonitoring, setIsMonitoring] = useState(false);
  const [currentCall, setCurrentCall] = useState(null);
  const [alerts, setAlerts] = useState([]);
  const [riskScore, setRiskScore] = useState(0);
  const [showTribute, setShowTribute] = useState(false);
  const [isRecording, setIsRecording] = useState(false);

  useEffect(() => {
    checkAndRequestPermissions();
    
    // Setup call state listener
    const callListener = CallDetector.addListener('CallStateChanged', handleCallStateChanged);
    
    // Setup recording status listener
    const recordingListener = AudioRecorder.addListener('recordingStatus', handleRecordingStatus);

    return () => {
      // Cleanup listeners
      CallDetector.removeAllListeners();
      AudioRecorder.removeAllListeners();
      if (isMonitoring) {
        stopMonitoring();
      }
    };
  }, []);

  const checkAndRequestPermissions = async () => {
    try {
      const status = await Permissions.checkPermissions();
      if (status.allGranted) {
        setPermissionsGranted(true);
        return;
      }

      const result = await Permissions.requestPermissions();
      if (!result) {
        Alert.alert(
          'Permissions Required',
          'This app needs permissions to protect you from scam calls. Please grant them in Settings.',
          [
            { text: 'Cancel', style: 'cancel' },
            { text: 'Open Settings', onPress: Permissions.openSettings }
          ]
        );
      } else {
        // Recheck permissions after request
        const newStatus = await Permissions.checkPermissions();
        setPermissionsGranted(newStatus.allGranted);
      }
    } catch (error) {
      console.error('Error with permissions:', error);
      Alert.alert('Error', 'Failed to check permissions');
    }
  };

  const handleCallStateChanged = (event) => {
    console.log('Call state changed:', event);
    
    const { callState, number, state } = event;
    
    if (callState === 'RINGING' || state === 'RINGING') {
      // Incoming call detected
      setCurrentCall({
        number: number || 'Unknown',
        startTime: new Date(),
        riskLevel: 'Analyzing...'
      });
      
      // Start recording for analysis
      startRecording();
      
      // Add alert
      addAlert('Incoming Call Detected', `Call from ${number || 'Unknown number'}`, 'info');
      
      // Simulate risk analysis
      setTimeout(() => {
        const risk = Math.floor(Math.random() * 100);
        setRiskScore(risk);
        setCurrentCall(prev => prev ? { ...prev, riskLevel: getRiskLevelText(risk) } : null);
        
        if (risk > 70) {
          addAlert('High Risk Call Detected!', 'This call shows signs of potential fraud', 'warning');
        }
      }, 2000);
      
    } else if (callState === 'IDLE' || state === 'IDLE') {
      // Call ended
      if (currentCall) {
        addAlert('Call Ended', `Call with ${currentCall.number} ended`, 'info');
      }
      setCurrentCall(null);
      setRiskScore(0);
      stopRecording();
    }
  };

  const handleRecordingStatus = (event) => {
    setIsRecording(event.recording);
  };

  const getRiskLevelText = (score) => {
    if (score < 30) return 'Low Risk';
    if (score < 70) return 'Medium Risk';
    return 'High Risk - Potential Scam';
  };

  const addAlert = (title, description, type = 'info') => {
    const newAlert = {
      id: Date.now(),
      title,
      description,
      time: new Date().toLocaleTimeString(),
      type
    };
    
    setAlerts(prevAlerts => [newAlert, ...prevAlerts.slice(0, 9)]); // Keep only last 10 alerts
  };

  const startRecording = async () => {
    try {
      await AudioRecorder.start();
      console.log('Recording started');
    } catch (error) {
      console.error('Failed to start recording:', error);
    }
  };

  const stopRecording = async () => {
    try {
      const filePath = await AudioRecorder.stop();
      console.log('Recording stopped, file saved to:', filePath);
    } catch (error) {
      console.error('Failed to stop recording:', error);
    }
  };

  const toggleMonitoring = async () => {
    if (!permissionsGranted) {
      await checkAndRequestPermissions();
      return;
    }

    if (!isMonitoring) {
      startMonitoring();
    } else {
      stopMonitoring();
    }
  };

  const startMonitoring = async () => {
    try {
      const hasPermission = await CallDetector.checkPermission();
      if (!hasPermission) {
        Alert.alert('Permission Required', 'Phone state permission is required for call monitoring');
        return;
      }

      await CallDetector.startCallDetection(handleCallStateChanged);
      setIsMonitoring(true);
      addAlert('Monitoring Started', 'Call protection is now active', 'success');
    } catch (error) {
      console.error('Error starting monitoring:', error);
      Alert.alert('Error', 'Failed to start call monitoring: ' + error.message);
    }
  };

  const stopMonitoring = async () => {
    try {
      await CallDetector.stopCallDetection();
      await AudioRecorder.stop().catch(() => {}); // Stop recording if active
      setIsMonitoring(false);
      setCurrentCall(null);
      setRiskScore(0);
      addAlert('Monitoring Stopped', 'Call protection is now inactive', 'info');
    } catch (error) {
      console.error('Error stopping monitoring:', error);
      Alert.alert('Error', 'Failed to stop call monitoring');
    }
  };

  const renderCurrentCall = () => {
    if (!currentCall) return null;

    return (
      <View style={styles.callCard}>
        <Text style={styles.callNumber}>Number: {currentCall.number}</Text>
        <Text style={[styles.riskLevel, getRiskLevelStyle(riskScore)]}>
          Risk Level: {currentCall.riskLevel}
        </Text>
        {isRecording && (
          <Text style={styles.recordingIndicator}>🔴 Recording for analysis</Text>
        )}
      </View>
    );
  };

  const getRiskLevelStyle = (score) => {
    if (score < 30) return { color: '#4CAF50' };
    if (score < 70) return { color: '#FF9800' };
    return { color: '#F44336' };
  };

  const renderAlerts = () => {
    return alerts.slice(0, 5).map((alert) => (
      <View key={alert.id} style={[styles.alertCard, getAlertStyle(alert.type)]}>
        <Text style={styles.alertTitle}>{alert.title}</Text>
        <Text style={styles.alertDescription}>{alert.description}</Text>
        <Text style={styles.alertTime}>{alert.time}</Text>
      </View>
    ));
  };

  const getAlertStyle = (type) => {
    switch (type) {
      case 'warning': return { borderLeftColor: '#FF9800', borderLeftWidth: 4 };
      case 'success': return { borderLeftColor: '#4CAF50', borderLeftWidth: 4 };
      case 'error': return { borderLeftColor: '#F44336', borderLeftWidth: 4 };
      default: return { borderLeftColor: '#2196F3', borderLeftWidth: 4 };
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.titleHH}>HH</Text>
        <Text style={styles.titleMain}>Hello Hari</Text>
        <Text style={styles.subtitle}>Your Call Safety Companion</Text>
        {Platform.OS === 'android' && (
          <Text style={styles.platformInfo}>Android Protection Active</Text>
        )}
      </View>

      {!permissionsGranted && (
        <View style={styles.permissionBanner}>
          <Text style={styles.permissionText}>⚠️ Permissions required for full protection</Text>
          <TouchableOpacity 
            style={styles.permissionButton}
            onPress={checkAndRequestPermissions}
          >
            <Text style={styles.permissionButtonText}>Grant Permissions</Text>
          </TouchableOpacity>
        </View>
      )}

      <TouchableOpacity 
        style={[
          styles.monitorButton, 
          isMonitoring && styles.monitoringActive,
          !permissionsGranted && styles.monitorButtonDisabled
        ]}
        onPress={toggleMonitoring}
        disabled={!permissionsGranted}
      >
        <Text style={styles.buttonText}>
          {isMonitoring ? 'Stop Monitoring' : 'Start Monitoring'}
        </Text>
      </TouchableOpacity>

      {renderCurrentCall()}

      <View style={styles.riskMeter}>
        <Text style={styles.riskTitle}>Current Risk Level</Text>
        <View style={styles.riskBar}>
          <View style={[
            styles.riskFill, 
            { 
              width: `${riskScore}%`,
              backgroundColor: riskScore < 30 ? '#4CAF50' : riskScore < 70 ? '#FF9800' : '#F44336'
            }
          ]} />
        </View>
        <Text style={styles.riskPercentage}>{riskScore}%</Text>
      </View>

      <View style={styles.alertsContainer}>
        <Text style={styles.alertsTitle}>Recent Alerts</Text>
        {alerts.length === 0 ? (
          <Text style={styles.noAlerts}>No alerts yet. Start monitoring to begin protection.</Text>
        ) : (
          renderAlerts()
        )}
      </View>
      
      <TouchableOpacity 
        style={styles.tributeButton}
        onPress={() => setShowTribute(true)}
      >
        <Text style={styles.tributeButtonText}>About Hello Hari</Text>
      </TouchableOpacity>

      <Modal
        animationType="slide"
        transparent={true}
        visible={showTribute}
        onRequestClose={() => setShowTribute(false)}
      >
        <View style={styles.tributeModalContainer}>
          <View style={styles.tributeContent}>
            <Text style={styles.tributeTitle}>In Memory of Hari</Text>
            <Text style={styles.tributeText}>
              Hello Hari (HH) is dedicated to the memory of Hari, whose spirit of protecting and helping others lives on through this app.
            </Text>
            <Text style={styles.tributeQuote}>
              "Protecting one person from fraud is like protecting an entire family from grief"
            </Text>
            <Text style={styles.tributeMission}>
              This app serves as a guardian, helping people stay safe from scams and frauds - a mission that would have made Hari proud.
            </Text>
            <TouchableOpacity 
              style={styles.closeButton}
              onPress={() => setShowTribute(false)}
            >
              <Text style={styles.closeButtonText}>Close</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F6FA',
  },
  header: {
    padding: 20,
    backgroundColor: '#2E3192',
    elevation: 2,
    alignItems: 'center',
  },
  titleHH: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#FFF',
    marginBottom: 4,
  },
  titleMain: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#FFF',
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 14,
    color: '#E0E0E0',
  },
  platformInfo: {
    fontSize: 12,
    color: '#B0B0B0',
    marginTop: 4,
  },
  permissionBanner: {
    backgroundColor: '#FFF3CD',
    padding: 12,
    margin: 16,
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: '#FF9800',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  permissionText: {
    flex: 1,
    color: '#856404',
    fontSize: 14,
  },
  permissionButton: {
    backgroundColor: '#FF9800',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 4,
  },
  permissionButtonText: {
    color: '#FFF',
    fontSize: 12,
    fontWeight: 'bold',
  },
  monitorButton: {
    margin: 16,
    padding: 16,
    backgroundColor: '#4CAF50',
    borderRadius: 8,
    alignItems: 'center',
  },
  monitoringActive: {
    backgroundColor: '#F44336',
  },
  monitorButtonDisabled: {
    backgroundColor: '#CCCCCC',
  },
  buttonText: {
    color: '#FFF',
    fontSize: 16,
    fontWeight: 'bold',
  },
  callCard: {
    margin: 16,
    padding: 16,
    backgroundColor: '#FFF',
    borderRadius: 8,
    elevation: 2,
    borderLeftWidth: 4,
    borderLeftColor: '#2196F3',
  },
  callNumber: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  riskLevel: {
    marginTop: 8,
    fontSize: 14,
    fontWeight: '500',
  },
  recordingIndicator: {
    marginTop: 8,
    fontSize: 12,
    color: '#F44336',
    fontWeight: 'bold',
  },
  riskMeter: {
    margin: 16,
    padding: 16,
    backgroundColor: '#FFF',
    borderRadius: 8,
    elevation: 1,
  },
  riskTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 8,
  },
  riskBar: {
    height: 8,
    backgroundColor: '#EEE',
    borderRadius: 4,
  },
  riskFill: {
    height: '100%',
    borderRadius: 4,
  },
  riskPercentage: {
    marginTop: 8,
    textAlign: 'right',
    fontWeight: 'bold',
  },
  alertsContainer: {
    flex: 1,
    margin: 16,
  },
  alertsTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 8,
  },
  noAlerts: {
    textAlign: 'center',
    color: '#666',
    fontStyle: 'italic',
    marginTop: 20,
  },
  alertCard: {
    padding: 16,
    backgroundColor: '#FFF',
    borderRadius: 8,
    marginBottom: 8,
    elevation: 1,
  },
  alertTitle: {
    fontSize: 14,
    fontWeight: 'bold',
  },
  alertDescription: {
    marginTop: 4,
    color: '#666',
  },
  alertTime: {
    marginTop: 4,
    color: '#999',
    fontSize: 12,
  },
  tributeButton: {
    margin: 16,
    padding: 12,
    backgroundColor: '#2E3192',
    borderRadius: 8,
    alignItems: 'center',
  },
  tributeButtonText: {
    color: '#FFF',
    fontSize: 14,
    fontWeight: '500',
  },
  tributeModalContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0,0,0,0.5)',
  },
  tributeContent: {
    width: '90%',
    backgroundColor: '#FFF',
    borderRadius: 16,
    padding: 24,
    alignItems: 'center',
  },
  tributeTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#2E3192',
    marginBottom: 16,
  },
  tributeText: {
    fontSize: 16,
    color: '#333',
    textAlign: 'center',
    marginBottom: 16,
    lineHeight: 24,
  },
  tributeQuote: {
    fontSize: 18,
    fontStyle: 'italic',
    color: '#2E3192',
    textAlign: 'center',
    marginVertical: 16,
    paddingHorizontal: 20,
  },
  tributeMission: {
    fontSize: 16,
    color: '#333',
    textAlign: 'center',
    marginBottom: 24,
    lineHeight: 24,
  },
  closeButton: {
    paddingVertical: 12,
    paddingHorizontal: 24,
    backgroundColor: '#2E3192',
    borderRadius: 8,
  },
  closeButtonText: {
    color: '#FFF',
    fontSize: 16,
    fontWeight: '500',
  },
});

export default App;
