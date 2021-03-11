import { DataSourcePluginOptionsEditorProps } from '@grafana/data';
import { LegacyForms } from '@grafana/ui';
import React, { ChangeEvent, PureComponent } from 'react';

import { MyDataSourceOptions } from './types';

const { FormField, SecretFormField } = LegacyForms;

export class ConfigEditor extends PureComponent<DataSourcePluginOptionsEditorProps<MyDataSourceOptions>> {
  constructor(props: DataSourcePluginOptionsEditorProps<MyDataSourceOptions>) {
    super(props);
    if (props.options.jsonData != null && props.options.jsonData.timebaseUrl != null) {
      this.onTimebaseUrlChange(props.options.jsonData.timebaseUrl);
    }
  }
  onChangeUrl = (event: ChangeEvent<HTMLInputElement>) => {
    this.props.onOptionsChange({
      ...this.props.options,
      url: event.target.value,
      jsonData: {
        ...this.props.options.jsonData,
        timebaseUrl: event.target.value,
      },
    });
  };

  onTimebaseUrlChange = (timebaseUrl: string) => {
    this.props.onOptionsChange({
      ...this.props.options,
      jsonData: {
        ...this.props.options.jsonData,
        timebaseUrl,
      },
    });
  };

  onTimebaseUserChange = (event: ChangeEvent<HTMLInputElement>) => {
    this.props.onOptionsChange({
      ...this.props.options,
      jsonData: {
        ...this.props.options.jsonData,
        timebaseUser: event.target.value,
      },
    });
  };

  onTimebasePasswordChange = (event: ChangeEvent<HTMLInputElement>) => {
    this.props.onOptionsChange({
      ...this.props.options,
      secureJsonData: {
        ...this.props.options.secureJsonData,
        timebasePassword: event.target.value,
      },
    });
  };

  onReset = (config: any) => {};

  render() {
    return (
      <div className="gf-form-group">
        <div className="gf-form-group">
          <h3 className="page-heading">HTTP</h3>
          <div className="gf-form-group">
            <div className="gf-form">
              <FormField
                label="URL"
                width={30}
                value={this.props.options.url}
                onChange={this.onChangeUrl}
                type="string"
                placeholder="URL"
              />
            </div>
          </div>
        </div>
        <div className="gf-form">
          <div className="gf-form-group">
            <h3 className="page-heading">TimeBase Web Admin credentials</h3>
            <div className="gf-form-group">
              <div className="gf-form">
                <FormField
                  label="User"
                  width={30}
                  value={this.props.options.jsonData.timebaseUser}
                  onChange={this.onTimebaseUserChange}
                  type="string"
                  placeholder="username"
                />
              </div>
              <div className="gf-form">
                <SecretFormField
                  label="Password"
                  width={30}
                  onChange={this.onTimebasePasswordChange}
                  onReset={this.onReset}
                  isConfigured={false}
                  placeholder="password"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
}
