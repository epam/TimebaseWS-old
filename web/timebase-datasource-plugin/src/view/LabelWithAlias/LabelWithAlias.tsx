import { SegmentInput, Tooltip } from '@grafana/ui';
import { css, cx } from 'emotion';
import React, { PureComponent, RefObject } from 'react';

import { SegmentFrame } from '../SegmentFrame/SegmentFrame';

const styles = css`
  height: 32px;
  background-color: #202226;
  .gf-form-label-mr {
    margin-right: 0 !important;
  }
  .gf-form-segment-input {
    padding-left: 1px;
    background-color: inherit;
    margin-right: 0;
  }
  .gf-form-p {
    padding-left: 0px;
    padding-right: 4px;
  }
  .gf-form-pr-4 {
    padding-right: 4px;
    padding-left: 4px;
  }
  &.gf-form-mr-4 {
    margin-right: 4px;
  }
`;

styles;
interface LabelWithAliasProps {
  index: number;
  label: string;
  alias?: string | null;
  needBrackets?: boolean;
  additionalText?: string;
  className?: string;
  doc: string;
  onRemove: (item: string, index: number) => any;
  onChangeAlias: (index: number, aggregation: string, alias: string | null) => any;
}
interface LabelWithAliasState {
  showContextMenu: boolean;
  showAlias: boolean;
}

export class LabelWithAliasComponent extends PureComponent<LabelWithAliasProps, LabelWithAliasState> {
  state = {
    showContextMenu: false,
    showAlias: false,
  };

  wrapperRef: RefObject<any>;

  constructor(props: LabelWithAliasProps) {
    super(props);

    this.wrapperRef = React.createRef();
    this.handleClickOutside = this.handleClickOutside.bind(this);
  }

  static getDerivedStateFromProps(nextProps: LabelWithAliasProps) {
    return {
      showAlias: nextProps.alias != null,
    };
  }

  componentDidMount() {
    document.addEventListener('mousedown', this.handleClickOutside);
  }

  componentWillUnmount() {
    document.removeEventListener('mousedown', this.handleClickOutside);
  }

  handleClickOutside(event: any) {
    if (this.wrapperRef && !this.wrapperRef.current.contains(event.target) && this.state.showContextMenu) {
      this.setState({ showContextMenu: false });
    }
  }

  showMenu = () => {
    this.setState({ ...this.state, showContextMenu: true });
  };

  remove = () => {
    this.props.onRemove(this.props.label, this.props.index);
    this.setState({ ...this.state, showContextMenu: false });
  };

  changeAlias = (alias: React.ReactText) => {
    this.props.onChangeAlias(this.props.index, this.props.label, alias.toString());
  };

  addAlias = () => {
    this.setState({ ...this.state, showContextMenu: false });
    this.props.onChangeAlias(this.props.index, this.props.label, this.props.label.toLowerCase().toString());
  };

  removeAlias = () => {
    this.setState({ ...this.state, showContextMenu: false });
    this.props.onChangeAlias(this.props.index, this.props.label, null);
  };
  getContent = () => {
    return (
      <div
        className={cx(
          `gf-form-label gf-form-label-mr gf-form-pr-4 ${this.props.className != null ? this.props.className : ''}`,
          styles
        )}
        onClick={this.showMenu}
      >
        {this.props.label}
        {this.props.needBrackets ? '()' : ''}
      </div>
    );
  };

  render() {
    return (
      <div ref={this.wrapperRef} className={cx('dropdown open pointer gf-form-mr-4', styles)}>
        <div className={cx('gf-form', styles)}>
          {this.props.doc !== '' && this.props.doc != null ? (
            <Tooltip content={this.props.doc} theme="info">
              {this.getContent()}
            </Tooltip>
          ) : (
            this.getContent()
          )}
          {this.state.showAlias ? (
            <div className="gf-form-inline">
              <div className={cx('gf-form-label gf-form-label-mr gf-form-p', styles)}> as </div>
              <SegmentFrame className="gf-form-inline" hideShadow={true}>
                <SegmentInput
                  className={cx('gf-form-segment-input', styles)}
                  value={this.props.alias as string}
                  onChange={this.changeAlias}
                />
              </SegmentFrame>
            </div>
          ) : null}
        </div>

        {this.state.showContextMenu ? (
          <ul className="dropdown-menu">
            {this.state.showAlias ? (
              <li className="dropdown-menu-item" onClick={this.removeAlias}>
                <a>Remove alias</a>
              </li>
            ) : (
              <li className="dropdown-menu-item" onClick={this.addAlias}>
                <a>Add alias</a>
              </li>
            )}
            <li className="dropdown-menu-item" onClick={this.remove}>
              <a>Remove {this.props.additionalText != null ? this.props.additionalText : ''}</a>
            </li>
          </ul>
        ) : null}
      </div>
    );
  }
}
