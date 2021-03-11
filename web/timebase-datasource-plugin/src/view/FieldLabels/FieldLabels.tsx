import { css, cx } from 'emotion';
import React, { PureComponent, RefObject } from 'react';

const styles = css`
  &.green {
    color: #7bc363;
    margin-right: 3px;
  }
  &.orange {
    color: #da9340;
  }
  &.blue {
    color: #61aef3;
    margin-left: 3px;
  }
  &.font-size-14 {
    font-size: 14px;
  }
`;

styles;

interface FieldLabelProps {
  value: string;
  type: string;
  needFiledFun: boolean;
  fun: string | undefined;
  index?: number;
  ignoreEv?: boolean;
  onRemove?: (index: number) => any;
}

export const ColoredFieldLabelComponent = (props: FieldLabelProps) => {
  return props.needFiledFun ? (
    <ColoredFieldWithFunctionComponent
      fun={props.fun as string}
      index={props.index as number}
      type={props.type}
      value={props.value}
      ignoreEv={props.ignoreEv}
      onRemove={props.onRemove as any}
    />
  ) : (
    <ColoredFieldComponent type={props.type} value={props.value} />
  );
};

const ColoredFieldComponent = ({ value, type }: any) => (
  <div className="gf-form-label pointer">
    <div className={cx('green', styles)}>{type}</div>
    {type !== '' && value !== '' ? <div>:</div> : null}
    <div className={cx('blue', styles)}>{value}</div>
  </div>
);

export const FieldLabelComponent = ({ value, needFiledFun, fun }: any) =>
  needFiledFun ? (
    <FieldWithFunctionComponent fun={fun} value={value} />
  ) : (
    <div className="gf-form-label pointer">{value}</div>
  );

const FieldWithFunctionComponent = ({ value, fun }: any) => (
  <div className="gf-form-label pointer">
    <div>{fun}</div>
    <div>(</div>
    <div>{value}</div>
    <div>)</div>
  </div>
);

interface ColoredFieldWithFunctionProps {
  value: string;
  fun: string;
  type: string;
  index: number;
  ignoreEv?: boolean;
  onRemove: (index: number) => any;
}

export class ColoredFieldWithFunctionComponent extends PureComponent<ColoredFieldWithFunctionProps> {
  state = {
    showRemove: false,
  };

  wrapperRef: RefObject<any>;

  constructor(props: ColoredFieldWithFunctionProps) {
    super(props);

    this.wrapperRef = React.createRef();
    this.handleClickOutside = this.handleClickOutside.bind(this);
  }

  componentDidMount() {
    document.addEventListener('mousedown', this.handleClickOutside);
  }

  componentWillUnmount() {
    document.removeEventListener('mousedown', this.handleClickOutside);
  }

  handleClickOutside(event: any) {
    if (this.wrapperRef && !this.wrapperRef.current.contains(event.target) && this.state.showRemove) {
      this.setState({ showRemove: false });
    }
  }

  showMenu = (e: any) => {
    this.setState({ showRemove: true });
    e.preventDefault();
    if (!this.props.ignoreEv) {
      e.stopPropagation();
    }
  };

  remove = (e: any) => {
    this.props.onRemove(this.props.index);
    this.setState({ showRemove: false });
    e.preventDefault();
    if (!this.props.ignoreEv) {
      e.stopPropagation();
    }
  };

  render() {
    return (
      <div ref={this.wrapperRef} className="gf-form-label pointer open">
        <div onClick={this.showMenu} className={cx('orange', styles)}>
          {this.props.fun}
        </div>
        <div>(</div>
        <div className={cx('green', styles)}>{this.props.type}</div>
        {this.props.type !== '' && this.props.value !== '' ? <div>:</div> : null}
        <div className={cx('blue', styles)}>{this.props.value}</div>
        <div>)</div>
        {this.state.showRemove ? (
          <ul className="dropdown-menu">
            <li className="dropdown-menu-item" onClick={this.remove}>
              <a className={cx('font-size-14', styles)}>Remove</a>
            </li>
          </ul>
        ) : null}
      </div>
    );
  }
}
