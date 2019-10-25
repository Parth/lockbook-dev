package lockbook.dev

import java.io.File
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import com.vladsch.flexmark.ast._
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.{Document, NodeVisitor, VisitHandler}
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.control._
import javafx.scene.input.{KeyCode, KeyCodeCombination, KeyCombination, KeyEvent}
import javafx.scene.layout.BorderPane
import org.eclipse.jgit.api.Git
import org.fxmisc.richtext.CodeArea

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
class EditorUi(editorHelper: EditorHelper, gitHelper: GitHelper, executor: ScheduledThreadPoolExecutor) {

  val parser: Parser = Parser.builder().build()

  def getView(git: Git, f: File): BorderPane = {
    val textArea  = new CodeArea
    val root      = new BorderPane
    val syncLabel = new Label("Loading File")

    loadFile(git, f, root, textArea, syncLabel)

    BorderPane.setAlignment(syncLabel, Pos.CENTER_RIGHT)
    syncLabel.setId("SyncStatus")
    root.setId("SyncStatusParent")
    root.bottomProperty().setValue(syncLabel)
    val autoSaveTask = saveCommitAndPushTask(syncLabel, textArea, f, git)
    val saveOnIdle   = CancelableAction(executor, FiniteDuration(1, TimeUnit.SECONDS), autoSaveTask)

    scheduleAutoSave(textArea, syncLabel, saveOnIdle)
    addSyncListener(root, git, syncLabel)

    root
  }

  private def addSyncListener(root: BorderPane, git: Git, label: Label): Unit = {
    val saveKeyCombo = new KeyCodeCombination(KeyCode.S, KeyCombination.META_DOWN)

    // Add shortcut listener when we're mounted to a scene
    root
      .sceneProperty()
      .addListener((_, _, newv) => {
        if (newv != null) {
          newv.addEventHandler(
            KeyEvent.KEY_PRESSED,
            (event: KeyEvent) => {
              if (saveKeyCombo.`match`(event)) {

                // Shortcut is actually matched here
                label.setText("Pushing changes...")
                Future {
                  gitHelper.commitAndPush("", git) match {
                    case Left(error) =>
                      Platform.runLater(() => AlertUi.showBad("Push Failed", error.uiMessage)) // TODO Does this need to be Platform.runLater?
                    case Right(_) =>
                      Platform.runLater(() => label.setText("Commit & Push Successful"))
                  }
                }

              }
            }
          )
        }
      })
  }

  private def loadFile(git: Git, f: File, root: BorderPane, text: CodeArea, syncLabel: Label): Future[Unit] = Future {
    editorHelper.getTextFromFile(f) match {
      case Right(fileText) =>
        Platform.runLater(() => {
          doMarkdown(text)
          text.setWrapText(true)
          root.setCenter(text)
          text.replaceText(fileText) // TODO Empty files makes this upset
          syncLabel.setText("File loaded successfully")
        })
      case Left(error) =>
        Platform.runLater(
          () => root.setCenter(new Label(error.uiMessage))
        )
    }
  }

  private def scheduleAutoSave(text: CodeArea, syncLabel: Label, saveTask: CancelableAction): Unit = {
    text
      .textProperty()
      .addListener((_, oldValue, _) => {
        if (oldValue != "") {
          syncLabel.setText("")
          saveTask.snooze()
        }
      })
  }

  private def saveCommitAndPushTask(l: Label, codeArea: CodeArea, file: File, git: Git): () => Unit = () => {
    Platform.runLater(() => l.setText("Saving..."))
    Future {
      editorHelper
        .save(codeArea.getText, file) match {
        case Right(_) =>
          Platform.runLater(() => {
            l.setText("Saved")
          })
        case Left(exception) =>
          Platform.runLater(() => {
            l.setText(s"Save Failed: ${exception.uiMessage}")
          })
      }
    }
  }

  private def renderMarkdownTask(codeArea: CodeArea): () => Unit = () => {
    val parsed: Document = parser.parse(codeArea.getText())
    Platform.runLater(() => nodeVisitor(codeArea).visit(parsed))
  }

  private def doMarkdown(text: CodeArea): Unit = {
    val markdownTask =
      CancelableAction(executor, FiniteDuration(200, TimeUnit.MILLISECONDS), renderMarkdownTask(text)) // Good settings candidate

    text
      .textProperty()
      .addListener((_, _, _) => {
        markdownTask.snooze()
      })
  }

  private val nodeVisitor: CodeArea => NodeVisitor = (styledText: CodeArea) =>
    new NodeVisitor(
      new VisitHandler[Text](
        classOf[Text],
        (node: Text) =>
          styledText.setStyleClass(node.getStartOffset, node.getEndOffset, "text")
      ),
      new VisitHandler[Heading](
        classOf[Heading],
        (node: Heading) =>
          styledText.setStyleClass(node.getOpeningMarker.getStartOffset, node.getText.getEndOffset, s"h${node.getLevel}")
      ),
      new VisitHandler[Code](
        classOf[Code],
        (node: Code) =>
          styledText.setStyleClass(node.getOpeningMarker.getStartOffset, node.getClosingMarker.getEndOffset, "code")
      ),
      new VisitHandler[Emphasis](
        classOf[Emphasis],
        (node: Emphasis) =>
          styledText.setStyleClass(node.getOpeningMarker.getStartOffset, node.getClosingMarker.getEndOffset, "emphasis")
      ),
      new VisitHandler[FencedCodeBlock](
        classOf[FencedCodeBlock],
        (node: FencedCodeBlock) =>
          if (node.getClosingFence.getEndOffset != 0) {
            styledText.setStyleClass(node.getOpeningMarker.getStartOffset, node.getClosingMarker.getEndOffset, "code-block")
            formatCodeBlock(styledText, node)
          }
      ),
      new VisitHandler[BlockQuote](
        classOf[BlockQuote],
        (node: BlockQuote) =>
          styledText.setStyleClass(node.getStartOffset, node.getEndOffset, "quote-block")
      ),
      new VisitHandler[Link](
        classOf[Link],
        (node: Link) =>
          styledText.setStyleClass(node.getTextOpeningMarker.getStartOffset, node.getLinkClosingMarker.getEndOffset, "link")
      ),
      new VisitHandler[BulletListItem](
        classOf[BulletListItem],
        (node: BulletListItem) =>
          styledText.setStyleClass(node.getOpeningMarker.getStartOffset, node.getOpeningMarker.getEndOffset, "bullet")
      ),
      new VisitHandler[OrderedListItem](
        classOf[OrderedListItem],
        (node: OrderedListItem) =>
          styledText.setStyleClass(node.getOpeningMarker.getStartOffset, node.getOpeningMarker.getEndOffset, "number")
      )
    )

  private def formatCodeBlock(styledText: CodeArea, node: FencedCodeBlock) = {
    val code = styledText.getText(node.getOpeningMarker.getStartOffset, node.getClosingMarker.getEndOffset)
    val keywords = Set("abstract", "case", "class", "def", "extends", "match")
    var lastEnd = node.getOpeningMarker.getStartOffset
    code.split("(?=\\s+)").map(w => {
      if (keywords.contains(w.trim)) styledText.setStyleClass(lastEnd, lastEnd+w.length, "fruity")

      lastEnd += w.length
    })
  }
}
