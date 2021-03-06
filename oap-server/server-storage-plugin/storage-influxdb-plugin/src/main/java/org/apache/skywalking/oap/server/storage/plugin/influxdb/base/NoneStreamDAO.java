/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.storage.plugin.influxdb.base;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.skywalking.apm.commons.datacarrier.common.AtomicRangeInteger;
import org.apache.skywalking.oap.server.core.analysis.TimeBucket;
import org.apache.skywalking.oap.server.core.analysis.config.NoneStream;
import org.apache.skywalking.oap.server.core.profile.ProfileTaskRecord;
import org.apache.skywalking.oap.server.core.storage.INoneStreamDAO;
import org.apache.skywalking.oap.server.core.storage.StorageBuilder;
import org.apache.skywalking.oap.server.core.storage.model.Model;
import org.apache.skywalking.oap.server.storage.plugin.influxdb.InfluxClient;
import org.influxdb.dto.Point;

public class NoneStreamDAO implements INoneStreamDAO {
    public static final String TAG_SERVICE_ID = "_service_id";
    private static final int PADDING_SIZE = 1_000_000;
    private static final AtomicRangeInteger SUFFIX = new AtomicRangeInteger(0, PADDING_SIZE);

    private InfluxClient client;
    private StorageBuilder<NoneStream> storageBuilder;

    public NoneStreamDAO(InfluxClient client, StorageBuilder<NoneStream> storageBuilder) {
        this.client = client;
        this.storageBuilder = storageBuilder;
    }

    @Override
    public void insert(final Model model, final NoneStream noneStream) throws IOException {
        final long timestamp = TimeBucket.getTimestamp(
            noneStream.getTimeBucket(), model.getDownsampling()) * PADDING_SIZE + SUFFIX.getAndIncrement();

        Point point = new InfluxInsertRequest(model, noneStream, storageBuilder)
            .time(timestamp, TimeUnit.NANOSECONDS)
            .addFieldAsTag(ProfileTaskRecord.SERVICE_ID, TAG_SERVICE_ID).getPoint();

        client.write(point);
    }
}
