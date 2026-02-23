import React from 'react';
import { Routes, Route } from 'react-router-dom';

const App: React.FC = () => {
  return (
    <Routes>
      <Route path="/" element={<div>CollabStack Cloud Storage</div>} />
    </Routes>
  );
};

export default App;
