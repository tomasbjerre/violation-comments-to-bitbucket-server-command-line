package se.bjurr.violations.main;

import static se.bjurr.violations.comments.bitbucketserver.lib.ViolationCommentsToBitbucketServerApi.violationCommentsToBitbucketServerApi;
import static se.bjurr.violations.lib.ViolationsApi.violationsApi;
import static se.bjurr.violations.lib.model.SEVERITY.INFO;
import static se.softhouse.jargo.Arguments.booleanArgument;
import static se.softhouse.jargo.Arguments.enumArgument;
import static se.softhouse.jargo.Arguments.helpArgument;
import static se.softhouse.jargo.Arguments.integerArgument;
import static se.softhouse.jargo.Arguments.optionArgument;
import static se.softhouse.jargo.Arguments.stringArgument;
import static se.softhouse.jargo.CommandLineParser.withArguments;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import se.bjurr.violations.comments.bitbucketserver.lib.ViolationCommentsToBitbucketServerApi;
import se.bjurr.violations.comments.lib.ViolationsLogger;
import se.bjurr.violations.lib.model.SEVERITY;
import se.bjurr.violations.lib.model.Violation;
import se.bjurr.violations.lib.reports.Parser;
import se.bjurr.violations.lib.util.Filtering;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.ParsedArguments;

public class Runner {

  private List<List<String>> violations;
  private boolean commentOnlyChangedContent;
  private boolean createCommentWithAllSingleFileComments;
  private boolean createSingleFileComments;
  private SEVERITY minSeverity;
  private Boolean keepOldComments;
  private String commentTemplate;

  private Integer pullRequestId;
  private String projectKey;
  private String repoSlug;
  private String bitbucketServerUrl;
  private String proxyHost;
  private Integer proxyPort;
  private String proxyUser;
  private String proxyPass;
  private String username;
  private String password;
  private String personalAccessToken;
  private boolean createSingleFileCommentsTasks;
  private int commentOnlyChangedContentContext;
  private Integer maxNumberOfViolations;

  public void main(final String args[]) throws Exception {
    final Argument<?> helpArgument = helpArgument("-h", "--help");
    final String parsersString =
        Arrays.asList(Parser.values())
            .stream()
            .map((it) -> it.toString())
            .collect(Collectors.joining(", "));
    final Argument<List<List<String>>> violationsArg =
        stringArgument("--violations", "-v")
            .arity(4)
            .repeated()
            .description(
                "The violations to look for. <PARSER> <FOLDER> <REGEXP PATTERN> <NAME> where PARSER is one of: "
                    + parsersString
                    + "\n Example: -v \"JSHINT\" \".\" \".*/jshint.xml$\" \"JSHint\"")
            .build();
    final Argument<SEVERITY> minSeverityArg =
        enumArgument(SEVERITY.class, "-severity", "-s")
            .defaultValue(INFO)
            .description("Minimum severity level to report.")
            .build();
    final Argument<Boolean> showDebugInfo =
        optionArgument("-show-debug-info")
            .description(
                "Please run your command with this parameter and supply output when reporting bugs.")
            .build();

    final Argument<Boolean> commentOnlyChangedContentArg =
        booleanArgument("-comment-only-changed-content", "-cocc").defaultValue(true).build();
    final Argument<Boolean> createCommentWithAllSingleFileCommentsArg =
        booleanArgument("-create-comment-with-all-single-file-comments", "-ccwasfc")
            .defaultValue(false)
            .build();
    final Argument<Boolean> createSingleFileCommentsArg =
        booleanArgument("-create-single-file-comments", "-csfc").defaultValue(true).build();
    final Argument<Boolean> keepOldCommentsArg =
        booleanArgument("-keep-old-comments").defaultValue(false).build();
    final Argument<String> commentTemplateArg =
        stringArgument("-comment-template")
            .defaultValue("")
            .description("https://github.com/tomasbjerre/violation-comments-lib")
            .build();
    final Argument<Integer> pullRequestIdArg =
        integerArgument("-pull-request-id", "-prid").required().build();
    final Argument<String> projectKeyArg = stringArgument("-project-key", "-pk").required().build();
    final Argument<String> repoSlugArg = stringArgument("-repo-slug", "-rs").required().build();
    final Argument<String> bitbucketServerUrlArg =
        stringArgument("-server-url", "-url").required().build();
    final Argument<String> proxyHostArg = stringArgument("-proxy-host").defaultValue("").build();
    final Argument<Integer> proxyPortArg = integerArgument("-proxy-port").defaultValue(0).build();
    final Argument<String> proxyUserArg = stringArgument("-proxy-user").defaultValue("").build();
    final Argument<String> proxyPassArg =
        stringArgument("-proxy-password").defaultValue("").build();
    final Argument<String> usernameArg = stringArgument("-username").defaultValue("").build();
    final Argument<String> passwordArg = stringArgument("-password").defaultValue("").build();
    final Argument<String> personalAccessTokenArg =
        stringArgument("-personal-access-token", "-pat").defaultValue("").build();
    final Argument<Boolean> createSingleFileCommentsTasksArg =
        booleanArgument("-create-single-file-comments-tasks", "-csfct").defaultValue(false).build();
    final Argument<Integer> commentOnlyChangedContentContextArg =
        integerArgument("-comment-only-changed-content-context", "-coccc").defaultValue(5).build();
    final Argument<Integer> maxNumberOfViolationsArg =
        integerArgument("-max-number-of-violations", "-max")
            .defaultValue(Integer.MAX_VALUE)
            .build();

    try {
      final ParsedArguments parsed =
          withArguments( //
                  helpArgument, //
                  violationsArg, //
                  minSeverityArg, //
                  showDebugInfo, //
                  commentOnlyChangedContentArg, //
                  createCommentWithAllSingleFileCommentsArg, //
                  createSingleFileCommentsArg, //
                  keepOldCommentsArg, //
                  commentTemplateArg, //
                  proxyUserArg, //
                  pullRequestIdArg, //
                  projectKeyArg, //
                  repoSlugArg, //
                  bitbucketServerUrlArg, //
                  proxyHostArg, //
                  proxyPortArg, //
                  proxyPassArg, //
                  usernameArg, //
                  passwordArg, //
                  personalAccessTokenArg, //
                  createSingleFileCommentsTasksArg, //
                  commentOnlyChangedContentContextArg, //
                  maxNumberOfViolationsArg //
                  ) //
              .parse(args);

      this.violations = parsed.get(violationsArg);
      this.minSeverity = parsed.get(minSeverityArg);
      this.commentOnlyChangedContent = parsed.get(commentOnlyChangedContentArg);
      this.createCommentWithAllSingleFileComments =
          parsed.get(createCommentWithAllSingleFileCommentsArg);
      this.createSingleFileComments = parsed.get(createSingleFileCommentsArg);
      this.keepOldComments = parsed.get(keepOldCommentsArg);
      this.commentTemplate = parsed.get(commentTemplateArg);
      this.proxyUser = parsed.get(proxyUserArg);

      pullRequestId = parsed.get(pullRequestIdArg);
      this.projectKey = parsed.get(projectKeyArg);
      this.repoSlug = parsed.get(repoSlugArg);
      this.bitbucketServerUrl = parsed.get(bitbucketServerUrlArg);
      this.proxyHost = parsed.get(proxyHostArg);
      this.proxyPort = parsed.get(proxyPortArg);
      this.proxyPass = parsed.get(proxyPassArg);
      this.username = parsed.get(usernameArg);
      this.password = parsed.get(passwordArg);
      this.personalAccessToken = parsed.get(personalAccessTokenArg);
      this.createSingleFileCommentsTasks = parsed.get(createSingleFileCommentsTasksArg);
      this.commentOnlyChangedContentContext = parsed.get(commentOnlyChangedContentContextArg);
      this.maxNumberOfViolations = parsed.get(maxNumberOfViolationsArg);

      if (parsed.wasGiven(showDebugInfo)) {
        System.out.println(
            "Given parameters:\n"
                + Arrays.asList(args)
                    .stream()
                    .map((it) -> it.toString())
                    .collect(Collectors.joining(", "))
                + "\n\nParsed parameters:\n"
                + this.toString());
      }

    } catch (final ArgumentException exception) {
      System.out.println(exception.getMessageAndUsage());
      System.exit(1);
    }

    List<Violation> allParsedViolations = new ArrayList<>();
    for (final List<String> configuredViolation : violations) {
      final String reporter = configuredViolation.size() >= 4 ? configuredViolation.get(3) : null;
      final List<Violation> parsedViolations =
          violationsApi() //
              .findAll(Parser.valueOf(configuredViolation.get(0))) //
              .inFolder(configuredViolation.get(1)) //
              .withPattern(configuredViolation.get(2)) //
              .withReporter(reporter) //
              .violations();
      if (minSeverity != null) {
        allParsedViolations = Filtering.withAtLEastSeverity(allParsedViolations, minSeverity);
      }
      allParsedViolations.addAll(parsedViolations);
    }

    System.out.println(
        "PR: " + projectKey + "/" + repoSlug + "/" + pullRequestId + " on " + bitbucketServerUrl);
    final ViolationCommentsToBitbucketServerApi violationCommentsToBitbucketServerApi =
        violationCommentsToBitbucketServerApi();
    if (!proxyHost.isEmpty()) {
      violationCommentsToBitbucketServerApi //
          .withProxyHostNameOrIp(proxyHost) //
          .withProxyHostPort(proxyPort) //
          .withProxyUser(proxyUser) //
          .withProxyPassword(proxyPass);
    }
    try {
      if (!username.isEmpty()) {
        violationCommentsToBitbucketServerApi //
            .withUsername(username) //
            .withPassword(password);
      } else if (!personalAccessToken.isEmpty()) {
        violationCommentsToBitbucketServerApi //
            .withPersonalAccessToken(personalAccessToken);
      }

      violationCommentsToBitbucketServerApi //
          .withBitbucketServerUrl(bitbucketServerUrl) //
          .withPullRequestId(pullRequestId) //
          .withProjectKey(projectKey) //
          .withRepoSlug(repoSlug) //
          .withViolations(allParsedViolations) //
          .withCreateCommentWithAllSingleFileComments(createCommentWithAllSingleFileComments) //
          .withCreateSingleFileComments(createSingleFileComments) //
          .withCreateSingleFileCommentsTasks(createSingleFileCommentsTasks) //
          .withCommentOnlyChangedContent(commentOnlyChangedContent) //
          .withCommentOnlyChangedContentContext(commentOnlyChangedContentContext) //
          .withShouldKeepOldComments(keepOldComments) //
          .withCommentTemplate(commentTemplate) //
          .withMaxNumberOfViolations(maxNumberOfViolations) //
          .withViolationsLogger(
              new ViolationsLogger() {
                @Override
                public void log(final Level level, final String string) {
                  System.out.println(level + " " + string);
                }

                @Override
                public void log(final Level level, final String string, final Throwable t) {
                  final StringWriter sw = new StringWriter();
                  t.printStackTrace(new PrintWriter(sw));
                  System.out.println(level + " " + string + "\n" + sw.toString());
                }
              }) //
          .toPullRequest();
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public String toString() {
    return "Runner [violations="
        + violations
        + ", commentOnlyChangedContent="
        + commentOnlyChangedContent
        + ", createCommentWithAllSingleFileComments="
        + createCommentWithAllSingleFileComments
        + ", createSingleFileComments="
        + createSingleFileComments
        + ", minSeverity="
        + minSeverity
        + ", keepOldComments="
        + keepOldComments
        + ", commentTemplate="
        + commentTemplate
        + ", pullRequestId="
        + pullRequestId
        + ", projectKey="
        + projectKey
        + ", repoSlug="
        + repoSlug
        + ", bitbucketServerUrl="
        + bitbucketServerUrl
        + ", proxyHost="
        + proxyHost
        + ", proxyPort="
        + proxyPort
        + ", proxyUser="
        + proxyUser
        + ", proxyPass="
        + proxyPass
        + ", username="
        + username
        + ", password="
        + password
        + ", personalAccessToken="
        + personalAccessToken
        + ", createSingleFileCommentsTasks="
        + createSingleFileCommentsTasks
        + ", commentOnlyChangedContentContext="
        + commentOnlyChangedContentContext
        + ", maxNumberOfViolations="
        + maxNumberOfViolations
        + "]";
  }
}
