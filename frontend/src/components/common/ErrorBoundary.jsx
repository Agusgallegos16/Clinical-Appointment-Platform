import React, { Component } from 'react';
import ErrorFallback from './ErrorFallback';

class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
    };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    this.setState({ errorInfo });
    // Registrar error para monitoreo sin exponer PII
    console.error('⚠️ [ErrorBoundary] Excepción capturada en árbol de componentes:', error, errorInfo);
    if (this.props.onError) {
      this.props.onError(error, errorInfo);
    }
  }

  componentDidUpdate(prevProps) {
    // Si cambian las claves de reinicio (ej. cambio de ruta o de ID), reiniciar estado
    if (
      this.state.hasError &&
      this.props.resetKeys &&
      prevProps.resetKeys &&
      !this.areArraysEqual(prevProps.resetKeys, this.props.resetKeys)
    ) {
      this.resetErrorBoundary();
    }
  }

  areArraysEqual(a, b) {
    if (a.length !== b.length) return false;
    for (let i = 0; i < a.length; i++) {
      if (a[i] !== b[i]) return false;
    }
    return true;
  }

  resetErrorBoundary = () => {
    if (this.props.onReset) {
      this.props.onReset();
    }
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
    });
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        if (typeof this.props.fallback === 'function') {
          return this.props.fallback({
            error: this.state.error,
            errorInfo: this.state.errorInfo,
            resetErrorBoundary: this.resetErrorBoundary,
          });
        }
        return this.props.fallback;
      }

      return (
        <ErrorFallback
          error={this.state.error}
          errorInfo={this.state.errorInfo}
          resetErrorBoundary={this.resetErrorBoundary}
          isGlobal={this.props.isGlobal}
          title={this.props.title}
          message={this.props.message}
        />
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
