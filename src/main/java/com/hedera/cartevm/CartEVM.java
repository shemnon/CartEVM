package com.hedera.cartevm;

/*-
 * ‌
 * CartEVM
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
      names = {"--unrolled"},
      paramLabel = "int",
      description = "Number of unrolled iterations")
  private final Integer unrolled = 1;

  @CommandLine.Option(
      names = {"--loop"},
      paramLabel = "int",
      description = "Number of loop iterations")
  private final Integer loops = 10;

  @CommandLine.Option(
      names = {"--gas-limit"},
      paramLabel = "long",
      description = "Number of loop iterations")
  private final Long gasLimit = 10_000_000L;

  @CommandLine.Option(
      names = {"--steps"},
      paramLabel = "int",
      description = "Number of steps to combine per loop")
  private final Integer steps = 2;

  @CommandLine.Option(
      names = {"--filler"},
      description = "Generate Filler")
  private final Boolean filler = false;

  @CommandLine.Option(
      names = {"--output-dir"},
      paramLabel = "<dir>",
      description = "Directory to write Ethereum test fillers")
  private final File outDir = new File("vmCartEVM");

  @CommandLine.Option(
      names = {"--local"},
      description = "Execute in embedded EVM")
  private final Boolean local = false;

  @CommandLine.Option(
      names = {"--hedera"},
      description = "Execute hedera test instance")
  private final Boolean hedera = false;

  public static void main(String[] args) {
    CartEVM cartevm = new CartEVM();

    CommandLine commandLine = new CommandLine(cartevm);
    commandLine.execute(args);
  }

  public void runCase(List<Step> candidates, List<Step> chosen, int moreSteps) throws IOException {
    if (moreSteps < 1) {
      createFiller(chosen);
      runLocal(chosen);
    } else {
      for (Step step : candidates) {
        chosen.add(step);
        runCase(candidates, chosen, moreSteps - 1);
        chosen.remove(chosen.size() - 1);
      }
    }
  }

  private void createFiller(List<Step> chosen) throws IOException {
    if (!filler) {
      return;
    }
    FillerGenerator fillerGenerator = new FillerGenerator(chosen, unrolled, loops, gasLimit);
    System.out.println(fillerGenerator.getName());
    Path outputFile = outDir.toPath().resolve(fillerGenerator.getName() + "Filler.yml");
    System.out.println(outputFile);
    Files.writeString(outputFile, fillerGenerator.generate());
  }

  private void runLocal(List<Step> chosen) throws IOException {
    if (!local) {
      return;
    }
    new LocalRunner(chosen, unrolled, loops, gasLimit).execute();
  }

  @Override
  public void run() {
    try {
      runCase(Step.steps, new ArrayList<>(steps), steps);
      if (hedera) {
        System.out.println("Hedera Execution Not Implemented Yet");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
