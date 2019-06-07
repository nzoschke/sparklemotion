import React, { Component } from 'react';
import Draggable from 'react-draggable';
import classNames from 'classnames';
import styles from './FakeClientDevice.scss';
import PropTypes from 'prop-types';

const BORDER_WIDTH = 10;

export default class FakeClientDevice extends Component {
  constructor(props) {
    super(props);

    this.clientDeviceContentRef = React.createRef();
  }

  get width() {
    return this.props.width / 2;
  }

  get height() {
    return this.props.height / 2;
  }

  get containerStyle() {
    return {
      width: `${this.width + BORDER_WIDTH * 2}px`,
      height: `${this.height + BORDER_WIDTH * 2}px`,
    };
  }

  get contentStyle() {
    return {
      width: `100%`,
      height: `100%`,
    };
  }

  componentDidMount = () => {
    this.props.hostedWebApp.render(this.clientDeviceContentRef.current);
  };

  onZoomOut = () => {
    console.log('TODO: Implement onZoomOut');
  };

  onZoomIn = () => {
    console.log('TODO: Implement onZoomin');
  };

  onDragStart = (e, draggableData) => {
    const draggableNode = draggableData.node;
    const contentNode = this.clientDeviceContentRef.current;
    let eventNode = e.target;

    while (eventNode != null) {
      if (eventNode === contentNode) {
        return false;
      } else if (eventNode === draggableNode) {
        return true;
      } else {
        eventNode = eventNode.parentNode;
      }
    }
  };

  render() {
    return (
      <Draggable
        onStart={(e, data) => {
          return this.onDragStart(e, data);
        }}
      >
        <div
          className={styles['FakeClientDevice--pad']}
          style={this.containerStyle}
        >
          <div className={styles['FakeClientDevice-controls']}>
            <i
              className={classNames(
                'fas',
                'fa-search-minus',
                styles.iconButton
              )}
              onClick={this.onZoomOut}
            />
            <i
              className={classNames('fas', 'fa-search-plus', styles.iconButton)}
              onClick={this.onZoomIn}
            />
            <i
              className={classNames(
                'far',
                'fa-times-circle',
                styles.iconButton
              )}
              onClick={this.props.onClose}
            />
          </div>
          <div
            ref={this.clientDeviceContentRef}
            className={styles['FakeClientDevice--content']}
            style={this.contentStyle}
          />
        </div>
      </Draggable>
    );
  }
}

FakeClientDevice.propTypes = {
  width: PropTypes.number.isRequired,
  height: PropTypes.number.isRequired,
  hostedWebApp: PropTypes.object.isRequired,
  onClose: PropTypes.func.isRequired,
};
