/*
 * Copyright © 2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.apps.sentiment;

import co.cask.cdap.api.annotation.Batch;
import co.cask.cdap.api.annotation.ProcessInput;
import co.cask.cdap.api.annotation.UseDataSet;
import co.cask.cdap.api.dataset.lib.TimeseriesTable;
import co.cask.cdap.api.dataset.table.Increment;
import co.cask.cdap.api.dataset.table.Table;
import co.cask.cdap.api.flow.flowlet.AbstractFlowlet;
import co.cask.cdap.api.flow.flowlet.FlowletSpecification;
import co.cask.cdap.api.metrics.Metrics;
import com.google.common.base.Charsets;

import java.util.Iterator;

/**
 * Updates the timeseries table with sentiments received.
 */
public class CountSentimentFlowlet extends AbstractFlowlet {
  static final String NAME = "CountSentimentFlowlet";

  @UseDataSet(TwitterSentimentApp.TABLE_NAME)
  private Table sentiments;

  @UseDataSet(TwitterSentimentApp.TIMESERIES_TABLE_NAME)
  private TimeseriesTable textSentiments;

  Metrics metrics;

  @Batch(10)
  @ProcessInput
  public void process(Iterator<Tweet> sentimentItr) {
    while (sentimentItr.hasNext()) {
      Tweet tweet = sentimentItr.next();
      String sentence = tweet.getText();
      String sentiment = tweet.getSentiment();
      metrics.count("sentiment." + sentiment, 1);
      sentiments.increment(new Increment("aggregate", sentiment, 1));
      textSentiments.write(new TimeseriesTable.Entry(sentiment.getBytes(Charsets.UTF_8),
                                                     sentence.getBytes(Charsets.UTF_8),
                                                     System.currentTimeMillis()));

    }
  }

  @Override
  public FlowletSpecification configure() {
    return FlowletSpecification.Builder.with()
      .setName(NAME)
      .setDescription("Updates the sentiment counts")
      .build();
  }
}