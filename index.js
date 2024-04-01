import React, { useState, useEffect } from 'react';
import { AppRegistry, NativeModules, Text, View, Button } from 'react-native';

// Récupération du module natif
const { ScreenCaptureModule } = NativeModules;

const App = () => {
  const [isCapturing, setIsCapturing] = useState(false);

  const handleCaptureScreen = () => {
    setIsCapturing(true);
    ScreenCaptureModule.startCapture()
      .then(response => {
        console.log(response); // Afficher "Capture service started"
      })
      .catch(error => {
        console.error(error);
      });
  };

  const handleStopCapture = async () => {
    setIsCapturing(false);
    try {
      const response = await ScreenCaptureModule.stopCapture();
      console.log(response); // Afficher "Screen capture stopped"
    } catch (error) {
      console.error("Error stopping capture service: ", error);
    }
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <Text>Screen Capture Example</Text>
      {isCapturing ? (
        <Button title="Stop Capture" onPress={handleStopCapture} />
      ) : (
        <Button title="Start Capture" onPress={handleCaptureScreen} />
      )}
    </View>
  );
};


// Enregistrement du composant avec AppRegistry
AppRegistry.registerComponent('lappli_enfant', () => App);
