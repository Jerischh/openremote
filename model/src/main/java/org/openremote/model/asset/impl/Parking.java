/*
 * Copyright 2020, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model.asset.impl;

import org.openremote.model.Constants;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.v2.AttributeDescriptor;
import org.openremote.model.v2.MetaType;
import org.openremote.model.v2.ValueType;

import java.util.Optional;

public class Parking extends Device {

    public static final AttributeDescriptor<Integer> SPACES_TOTAL = new AttributeDescriptor<>("spacesTotal", true, ValueType.POSITIVE_INTEGER, null);
    public static final AttributeDescriptor<Integer> SPACES_OCCUPIED = new AttributeDescriptor<>("spacesOccupied", true, ValueType.POSITIVE_INTEGER, null);
    public static final AttributeDescriptor<Integer> SPACES_OPEN = new AttributeDescriptor<>("spacesOpen", true, ValueType.POSITIVE_INTEGER, null);
    public static final AttributeDescriptor<Integer> SPACES_BUFFER = new AttributeDescriptor<>("spacesBuffer", true, ValueType.POSITIVE_INTEGER, null);
    public static final AttributeDescriptor<Double> PRICE_HOURLY = new AttributeDescriptor<>("priceHourly", true, ValueType.POSITIVE_NUMBER, null,
        new MetaItem<>(MetaType.UNIT_TYPE, Constants.UNITS_CURRENCY_EUR)
    );
    public static final AttributeDescriptor<Double> PRICE_DAILY = new AttributeDescriptor<>("priceDaily", true, ValueType.POSITIVE_NUMBER, null,
        new MetaItem<>(MetaType.UNIT_TYPE, Constants.UNITS_CURRENCY_EUR)
    );

    public static final AssetDescriptor<Parking> DESCRIPTOR = new AssetDescriptor<>("parking", "0260ae", Parking.class);

    public Parking(String name) {
        super(name, DESCRIPTOR);
    }

    public Optional<Integer> getSpacesTotal() {
        return getAttributes().getValueOrDefault(SPACES_TOTAL);
    }

    public Optional<Integer> getSpacesOccupied() {
        return getAttributes().getValueOrDefault(SPACES_OCCUPIED);
    }

    public Optional<Integer> getSpacesOpen() {
        return getAttributes().getValueOrDefault(SPACES_OPEN);
    }

    public Optional<Integer> getSpacesBuffer() {
        return getAttributes().getValueOrDefault(SPACES_BUFFER);
    }

    public Optional<Double> getPriceHourly() {
        return getAttributes().getValueOrDefault(PRICE_HOURLY);
    }

    public Optional<Double> getPriceDaily() {
        return getAttributes().getValueOrDefault(PRICE_DAILY);
    }
}
