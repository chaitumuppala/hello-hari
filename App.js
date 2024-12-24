import React, { useState, useEffect } from 'react';
import { 
  SafeAreaView,
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  Modal
} from 'react-native';
import { Permissions } from './permissions';

const App = () => {
  const [permissionsGranted, setPermissionsGranted] = useState(false);
  const [isMonitoring, setIsMonitoring] = useState(false);
  const [currentCall, setCurrentCall] = useState(null);
  const [alerts, setAlerts] = useState([]);
  const [riskScore, setRiskScore] = useState(0);
  const [showTribute, setShowTribute] = useState(false);

  useEffect(() => {
    checkAndRequestPermissions();
  }, []);

  const checkAndRequestPermissions = async () => {
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
    setIsMonitoring(!isMonitoring);
  };

  const startMonitoring = () => {
    // Start the call recording service
    try {
      // Add the Call Recording Service implementation here
      setAlerts(prevAlerts => [
        {
          title: 'Monitoring Started',
          description: 'Call protection is now active',
          time: new Date().toLocaleTimeString()
        },
        ...prevAlerts
      ]);
    } catch (error) {
      Alert.alert('Error', 'Failed to start call monitoring');
      setIsMonitoring(false);
    }
  };

  const stopMonitoring = () => {
    // Stop the call recording service
    try {
      // Add the Call Recording Service stop implementation here
      setAlerts(prevAlerts => [
        {
          title: 'Monitoring Stopped',
          description: 'Call protection is now inactive',
          time: new Date().toLocaleTimeString()
        },
        ...prevAlerts
      ]);
    } catch (error) {
      Alert.alert('Error', 'Failed to stop call monitoring');
    }
  };

  const renderCurrentCall = () => {
    if (!currentCall) return null;

    return (
      <View style={styles.callCard}>
        <Text style={styles.callNumber}>Number: {currentCall.number}</Text>
        <Text style={styles.riskLevel}>Risk Level: {currentCall.riskLevel}</Text>
      </View>
    );
  };

  const renderAlerts = () => {
    return alerts.map((alert, index) => (
      <View key={index} style={styles.alertCard}>
        <Text style={styles.alertTitle}>{alert.title}</Text>
        <Text style={styles.alertDescription}>{alert.description}</Text>
        <Text style={styles.alertTime}>{alert.time}</Text>
      </View>
    ));
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.titleHH}>HH</Text>
        <Text style={styles.titleMain}>Hello Hari</Text>
        <Text style={styles.subtitle}>Your Call Safety Companion</Text>
      </View>

      <TouchableOpacity 
        style={[styles.monitorButton, isMonitoring && styles.monitoringActive]}
        onPress={toggleMonitoring}
      >
        <Text style={styles.buttonText}>
          {isMonitoring ? 'Stop Monitoring' : 'Start Monitoring'}
        </Text>
      </TouchableOpacity>

      {renderCurrentCall()}

      <View style={styles.riskMeter}>
        <Text style={styles.riskTitle}>Current Risk Level</Text>
        <View style={styles.riskBar}>
          <View style={[styles.riskFill, { width: `${riskScore}%` }]} />
        </View>
        <Text style={styles.riskPercentage}>{riskScore}%</Text>
      </View>

      <View style={styles.alertsContainer}>
        <Text style={styles.alertsTitle}>Recent Alerts</Text>
        {renderAlerts()}
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
    elevation: 1,
  },
  callNumber: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  riskLevel: {
    marginTop: 8,
    color: '#666',
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
    backgroundColor: '#4CAF50',
    borderRadius: 4,
  },
  riskPercentage: {
    marginTop: 8,
    textAlign: 'right',
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
