import { customElement, html, LitElement, property} from "lit-element";
import {
    AssetDescriptor,
    AttributePredicate,
    AssetQuery,
    RadialGeofencePredicate
} from "@openremote/model";
import {
    getAssetTypeFromQuery,
} from "../../index";
import "@openremote/or-input";
import {InputType} from "@openremote/or-input";
import i18next from "i18next";
import {OrRulesJsonRuleChangedEvent} from "../or-rule-json-viewer";
import {translate} from "@openremote/or-translate";

import {DialogAction, OrMwcDialog, OrMwcDialogOpenedEvent} from "@openremote/or-mwc-components/dist/or-mwc-dialog";
import {OrMap, OrMapClickedEvent} from '@openremote/or-map';
import '@openremote/or-map/dist/markers/or-map-marker';
import {LngLatLike} from "mapbox-gl";

@customElement("or-rule-radial-modal")
export class OrRuleRadialModal extends translate(i18next)(LitElement) {

    @property({type: Object})
    public assetDescriptor?: AssetDescriptor;

    @property({type: Object})
    public attributePredicate?: AttributePredicate;


    @property({type: Object})
    public query?: AssetQuery;
    
    constructor() {
        super();
        this.addEventListener(OrMwcDialogOpenedEvent.NAME, this.initRadialMap)
    }

    initRadialMap() {
        const modal = this.shadowRoot!.getElementById('radial-modal');
        if (!modal) return;

        const map = modal.shadowRoot!.querySelector('.or-map') as OrMap;
        if (map) {
            map.addEventListener(OrMapClickedEvent.NAME, (evt: CustomEvent) => {
                const lngLat:any = evt.detail.lngLat;
                const latElement = modal.shadowRoot!.querySelector('.location-lat') as HTMLInputElement;
                const lngElement = modal.shadowRoot!.querySelector('.location-lng') as HTMLInputElement;
                latElement.value = lngLat.lat;
                lngElement.value = lngLat.lng;
    
                const event = new Event('change');
                latElement.dispatchEvent(event);
                lngElement.dispatchEvent(event);
                this.setValuePredicateProperty('lat', lngLat.lat);
                this.setValuePredicateProperty('lng', lngLat.lng);
            });

            const latElement = modal.shadowRoot!.querySelector('.location-lat') as HTMLInputElement;
            const lngElement = modal.shadowRoot!.querySelector('.location-lng') as HTMLInputElement;
            if (lngElement.value && latElement.value) {
                const LngLat:LngLatLike = [parseFloat(lngElement.value), parseFloat(latElement.value)];
                map.flyTo(LngLat, 15)
            } else {
                map.flyTo();
            }
        }
    }

    protected getAttributeName(attributePredicate: AttributePredicate): string | undefined {
        return attributePredicate && attributePredicate.name ? attributePredicate.name.value : undefined;
    }

    protected setValuePredicateProperty(propertyName: string, value: any) {
        if(!this.attributePredicate) return;
        if(!this.attributePredicate.value) return;

        const valuePredicate = this.attributePredicate.value;

        (valuePredicate as any)[propertyName] = value;
        this.attributePredicate = {...this.attributePredicate};
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    renderDialogHTML(value:RadialGeofencePredicate) {
        const dialog: OrMwcDialog = this.shadowRoot!.getElementById("radial-modal") as OrMwcDialog;

        if (dialog) {
            dialog.dialogContent = html`
                <div style="display:grid">
                    <or-map class="or-map" type="VECTOR" style="border: 1px solid #d5d5d5; height: 400px; min-width: 300px;">
                        <or-map-marker active color="#FF0000" icon="information" lat="${value.lat}" lng="${value.lng}" radius="${value.radius}"></or-map-marker>
                    </or-map>
                
                    <div class="layout horizontal">
                        <input hidden class="location-lng"  required placeholder=" " type="text" .value="${value && value.lng ? value.lng : null}" />
                        <input hidden class="location-lat" required placeholder=" " type="text" .value="${value && value.lat ? value.lat : null}" />
                    </div>
                    
                    
                    <label>Straal (minimaal 100m)</label>     
                    <input @change="${(e:any) => this.setValuePredicateProperty("radius", parseInt(e.target.value))}" style="max-width: calc(50% - 30px);" required placeholder=" " min="100" type="number" .value="${value && value.radius ? value.radius : 100}" />
                </div>`;
        }
    }

    protected render() {
        if(!this.attributePredicate) return html``;
        if(!this.query) return html``;

        const valuePredicate = this.attributePredicate.value;
        if (!this.assetDescriptor || !valuePredicate) {
            return html``;
        }

        const attributeName = this.getAttributeName(this.attributePredicate);
        const assetType = getAssetTypeFromQuery(this.query);
        // @ts-ignore
        const value:RadialGeofencePredicate = valuePredicate ? valuePredicate : undefined;

        const radiusPickerModalActions: DialogAction[] = [
            {
                actionName: "ok",
                default: true,
                content: html`<or-input class="button" .type="${InputType.BUTTON}" .label="${i18next.t("ok")}"></or-input>`,
                action: () => {
                }
            },
            {
                actionName: "cancel",
                content: html`<or-input class="button" .type="${InputType.BUTTON}" .label="${i18next.t("cancel")}"></or-input>`,
                action: () => {
                    // Nothing to do here
                }
            },
        ];
       
      
        const radialPickerModalOpen = () => {
            const dialog: OrMwcDialog = this.shadowRoot!.getElementById("radial-modal") as OrMwcDialog;
            if (dialog) {
                dialog.open();
                this.renderDialogHTML(value);

            }
        };

        this.renderDialogHTML(value);
        
        return html`
            <or-input .type="${InputType.BUTTON}" .label="${i18next.t("area")}" @click="${radialPickerModalOpen}"></or-input>
            <or-mwc-dialog id="radial-modal" dialogTitle="area" .dialogActions="${radiusPickerModalActions}"></or-mwc-dialog>
        `
    }
}
