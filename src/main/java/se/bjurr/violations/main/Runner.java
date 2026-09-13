package se.bjurr.violations.main;

import static se.bjurr.violations.comments.bitbucketserver.lib.ViolationCommentsToBitbucketServerApi.violationCommentsToBitbucketServerApi;
import static se.bjurr.violations.lib.ViolationsApi.violationsApi;
import static se.bjurr.violations.lib.model.SEVERITY.INFO;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import se.bjurr.violations.comments.bitbucketserver.lib.ViolationCommentsToBitbucketServerApi;
import se.bjurr.violations.lib.FilteringViolationsLogger;
import se.bjurr.violations.lib.ViolationsLogger;
import se.bjurr.violations.lib.model.SEVERITY;
import se.bjurr.violations.lib.model.Violation;
import se.bjurr.violations.lib.reports.Parser;
import se.bjurr.violations.lib.util.Filtering;

@Command(name = "violation-comments-to-bitbucket-server-command-line")
public class Runner {

  @Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "Show this help message and exit.")
  private boolean help;

  @Option(
      names = {"--violations", "-v"},
      arity = "4",
      description =
          "The violations to look for. <PARSER> <FOLDER> <REGEXP PATTERN> <NAME> where PARSER"
              + " is one of the values of se.bjurr.violations.lib.reports.Parser (see supported"
              + " formats table in README for the full list).\nExample: -v \"JSHINT\" \".\""
              + " \".*/jshint.xml$\" \"JSHint\"")
  private List<String> violations = new ArrayList<>(); // NOPMD picocli reflection

  @Option(
      names = {"--ignorePaths", "-i"},
      description = "Ignore given paths\nExample: -i node_modules")
  private List<String> ignorePaths = new ArrayList<>(); // NOPMD picocli reflection

  @Option(
      names = {"-severity", "-s"},
      description = "Minimum severity level to report.")
  private SEVERITY minSeverity = INFO; // NOPMD picocli reflection

  @Option(
      names = "-show-debug-info",
      description =
          "Please run your command with this parameter and supply output when reporting bugs.")
  private boolean showDebugInfo;

  @Option(
      names = {"-comment-only-changed-content", "-cocc"},
      arity = "1")
  private boolean commentOnlyChangedContent = true; // NOPMD picocli reflection

  @Option(
      names = {"-comment-only-changed-files", "-cocf"},
      arity = "1",
      description =
          "True if only changed files should be commented. False if all findings should be commented.")
  private boolean commentOnlyChangedFiles = true; // NOPMD picocli reflection

  @Option(
      names = {"-create-comment-with-all-single-file-comments", "-ccwasfc"},
      arity = "1")
  private boolean createCommentWithAllSingleFileComments = false; // NOPMD picocli reflection

  @Option(
      names = {"-create-single-file-comments", "-csfc"},
      arity = "1")
  private boolean createSingleFileComments = true; // NOPMD picocli reflection

  @Option(names = "-keep-old-comments", arity = "1")
  private Boolean keepOldComments = false; // NOPMD picocli reflection

  @Option(
      names = "-comment-template",
      description = "https://github.com/tomasbjerre/violation-comments-lib")
  private String commentTemplate = ""; // NOPMD picocli reflection

  @Option(
      names = {"-pull-request-id", "-prid"},
      required = true)
  private Integer pullRequestId;

  @Option(
      names = {"-project-key", "-pk"},
      required = true)
  private String projectKey;

  @Option(
      names = {"-repo-slug", "-rs"},
      required = true)
  private String repoSlug;

  @Option(
      names = {"-server-url", "-url"},
      required = true)
  private String bitbucketServerUrl;

  @Option(names = "-proxy-host")
  private String proxyHost = ""; // NOPMD picocli reflection

  @Option(names = "-proxy-port")
  private Integer proxyPort = 0; // NOPMD picocli reflection

  @Option(names = "-proxy-user")
  private String proxyUser = ""; // NOPMD picocli reflection

  @Option(names = "-proxy-password")
  private String proxyPass = ""; // NOPMD picocli reflection

  @Option(names = "-username")
  private String username = ""; // NOPMD picocli reflection

  @Option(names = "-password")
  private String password = ""; // NOPMD picocli reflection

  @Option(names = {"-personal-access-token", "-pat"})
  private String personalAccessToken = ""; // NOPMD picocli reflection

  @Option(names = "-keystore-path")
  private String keyStorePath = ""; // NOPMD picocli reflection

  @Option(names = "-keystore-pass")
  private String keyStorePass = "changeit"; // NOPMD picocli reflection

  @Option(
      names = {"-create-single-file-comments-tasks", "-csfct"},
      arity = "1")
  private boolean createSingleFileCommentsTasks = false; // NOPMD picocli reflection

  @Option(names = {"-comment-only-changed-content-context", "-coccc"})
  private int commentOnlyChangedContentContext = 5; // NOPMD picocli reflection

  @Option(names = {"-max-number-of-violations", "-max"})
  private Integer maxNumberOfViolations = Integer.MAX_VALUE; // NOPMD picocli reflection

  public void main(final String... args) throws Exception {
    final CommandLine commandLine = new CommandLine(this);
    try {
      commandLine.parseArgs(args);
    } catch (final ParameterException exception) {
      System.out.println(exception.getMessage()); // NOPMD
      exception.getCommandLine().usage(System.out);
      System.exit(1); // NOPMD
      return;
    }

    if (commandLine.isUsageHelpRequested()) {
      commandLine.usage(System.out);
      return;
    }

    if (this.showDebugInfo) {
      System.out.println( // NOPMD
          "Given parameters:\n"
              + Arrays.asList(args).stream()
                  .map((it) -> it.toString())
                  .collect(Collectors.joining(", "))
              + "\n\nParsed parameters:\n"
              + this.toString());
    }

    ViolationsLogger violationsLogger =
        new ViolationsLogger() {
          @Override
          public void log(final Level level, final String string) {
            System.out.println(level + " " + string); // NOPMD
          }

          @Override
          @SuppressFBWarnings(
              value = "INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE",
              justification =
                  "Printing the stack trace to this CLI's own stdout is the intended behavior")
          public void log(final Level level, final String string, final Throwable t) {
            final StringWriter sw = new StringWriter();
            t.printStackTrace(
                new PrintWriter(sw)); // NOPMD writes to an in-memory buffer, not System.err
            System.out.println(level + " " + string + "\n" + sw.toString()); // NOPMD
          }
        };
    if (!this.showDebugInfo) {
      violationsLogger = FilteringViolationsLogger.filterLevel(violationsLogger);
    }

    Set<Violation> allParsedViolations = new TreeSet<>();
    for (int i = 0; i < this.violations.size(); i += 4) {
      final List<String> configuredViolation = this.violations.subList(i, i + 4);
      final String reporter = configuredViolation.get(3);
      final Set<Violation> parsedViolations =
          violationsApi() //
              .withViolationsLogger(violationsLogger) //
              .findAll(Parser.valueOf(configuredViolation.get(0))) //
              .inFolder(configuredViolation.get(1)) //
              .withPattern(configuredViolation.get(2)) //
              .withReporter(reporter) //
              .withIgnorePaths(this.ignorePaths) //
              .violations();
      if (this.minSeverity != null) {
        allParsedViolations = Filtering.withAtLEastSeverity(allParsedViolations, this.minSeverity);
      }
      allParsedViolations.addAll(parsedViolations);
    }

    System.out.println( // NOPMD
        "PR: "
            + this.projectKey
            + "/"
            + this.repoSlug
            + "/"
            + this.pullRequestId
            + " on "
            + this.bitbucketServerUrl);
    final ViolationCommentsToBitbucketServerApi violationCommentsToBitbucketServerApi =
        violationCommentsToBitbucketServerApi();
    if (!this.proxyHost.isEmpty()) {
      violationCommentsToBitbucketServerApi //
          .withProxyHostNameOrIp(this.proxyHost) //
          .withProxyHostPort(this.proxyPort) //
          .withProxyUser(this.proxyUser) //
          .withProxyPassword(this.proxyPass);
    }
    try {
      if (!this.keyStorePath.isEmpty()) {
        violationCommentsToBitbucketServerApi //
            .withKeyStorePath(this.keyStorePath) //
            .withKeyStorePass(this.keyStorePass);
      } else if (!this.username.isEmpty()) {
        violationCommentsToBitbucketServerApi //
            .withUsername(this.username) //
            .withPassword(this.password);
      } else if (!this.personalAccessToken.isEmpty()) {
        violationCommentsToBitbucketServerApi //
            .withPersonalAccessToken(this.personalAccessToken);
      }

      violationCommentsToBitbucketServerApi
          .withBitbucketServerUrl(this.bitbucketServerUrl)
          .withPullRequestId(this.pullRequestId)
          .withProjectKey(this.projectKey)
          .withRepoSlug(this.repoSlug)
          .withViolations(allParsedViolations)
          .withIgnorePaths(this.ignorePaths)
          .withCreateCommentWithAllSingleFileComments(this.createCommentWithAllSingleFileComments)
          .withCreateSingleFileComments(this.createSingleFileComments)
          .withCreateSingleFileCommentsTasks(this.createSingleFileCommentsTasks)
          .withCommentOnlyChangedContent(this.commentOnlyChangedContent)
          .withShouldCommentOnlyChangedFiles(this.commentOnlyChangedFiles)
          .withCommentOnlyChangedContentContext(this.commentOnlyChangedContentContext)
          .withShouldKeepOldComments(this.keepOldComments)
          .withCommentTemplate(this.commentTemplate)
          .withMaxNumberOfViolations(this.maxNumberOfViolations)
          .withViolationsLogger(violationsLogger) //
          .toPullRequest();
    } catch (final Exception e) {
      e.printStackTrace(); // NOPMD
    }
  }

  @Override
  public String toString() {
    return "Runner [violations="
        + this.violations
        + ", commentOnlyChangedContent="
        + this.commentOnlyChangedContent
        + ", commentOnlyChangedFiles="
        + this.commentOnlyChangedFiles
        + ", createCommentWithAllSingleFileComments="
        + this.createCommentWithAllSingleFileComments
        + ", createSingleFileComments="
        + this.createSingleFileComments
        + ", minSeverity="
        + this.minSeverity
        + ", keepOldComments="
        + this.keepOldComments
        + ", commentTemplate="
        + this.commentTemplate
        + ", ignorePaths="
        + this.ignorePaths
        + ", pullRequestId="
        + this.pullRequestId
        + ", projectKey="
        + this.projectKey
        + ", repoSlug="
        + this.repoSlug
        + ", bitbucketServerUrl="
        + this.bitbucketServerUrl
        + ", proxyHost="
        + this.proxyHost
        + ", proxyPort="
        + this.proxyPort
        + ", proxyUser="
        + this.proxyUser
        + ", proxyPass="
        + this.proxyPass
        + ", username="
        + this.username
        + ", password="
        + this.password
        + ", personalAccessToken="
        + this.personalAccessToken
        + ", keyStorePath="
        + this.keyStorePath
        + ", keyStorePass="
        + this.keyStorePass
        + ", createSingleFileCommentsTasks="
        + this.createSingleFileCommentsTasks
        + ", commentOnlyChangedContentContext="
        + this.commentOnlyChangedContentContext
        + ", maxNumberOfViolations="
        + this.maxNumberOfViolations
        + ", showDebugInfo="
        + this.showDebugInfo
        + "]";
  }
}
