package com.hedera.cartevm;

/*-
 * ‌
 * Hedera Services Node
 * ​
 * Copyright (C) 2021 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import picocli.CommandLine;

@CommandLine.Command(
    description = "Cartesian EVM test Generator.",
    abbreviateSynopsis = true,
    name = "cartevm",
    mixinStandardHelpOptions = true,
    sortOptions = false,
    header = "Usage:",
    synopsisHeading = "%n",
    descriptionHeading = "%nDescription:%n%n",
    optionListHeading = "%nOptions:%n",
    footerHeading = "%n",
    footer = "Hedera Hasgraph Services is licensed under the Apache License 2.0")
public class CartEVM implements Runnable {

  @CommandLine.Option(
      names = {"--inner-size"},
      paramLabel = "int",
      description = "Size of inner loop repetition")
  private final Integer innerSize = 10;

  public static int main(String[] args) {
    CartEVM cartevm = new CartEVM();

    CommandLine commandLine = new CommandLine(cartevm);
    return commandLine.execute(args);
  }

  @Override
  public void run() {}
}
