package lockbook.dev

import java.io.File
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import com.vladsch.flexmark.ast._
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.{Document, Node, NodeVisitor, VisitHandler}
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.control._
import javafx.scene.layout.BorderPane
import org.eclipse.jgit.api.Git
import org.fxmisc.richtext.CodeArea

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class EditorUi(editorHelper: EditorHelper, executor: ScheduledThreadPoolExecutor) {

  val parser: Parser = Parser.builder().build()

  def getView(git: Git, f: File): BorderPane = {
    val textArea  = new CodeArea
    val root      = new BorderPane
    val syncLabel = new Label("Loading File")

    loadFile(git, f, root, textArea, syncLabel)

    BorderPane.setAlignment(syncLabel, Pos.CENTER_RIGHT)
    syncLabel.setId("SyncStatus")
    root.bottomProperty().setValue(syncLabel)
    val autoSaveTask = saveCommitAndPushTask(syncLabel, textArea, f, git)
    scheduleAutoSave(textArea, syncLabel, autoSaveTask)

    root
  }

  private def loadFile(git: Git, f: File, root: BorderPane, text: CodeArea, syncLabel: Label): Future[Unit] = Future {
    editorHelper.getTextFromFile(f) match {
      case Right(fileText) =>
        Platform.runLater(() => {
          doMarkdown(text)
          text.setWrapText(true)
          root.setCenter(text)
          text.replaceText(fileText)
          syncLabel.setText("File loaded successfully")
        })
      case Left(error) =>
        Platform.runLater(
          () => root.setCenter(new Label(error.uiMessage))
        )
    }
  }

  private def scheduleAutoSave(text: CodeArea, syncLabel: Label, saveTask: () => Unit): Unit = {
    val saveOnIdle = CancelableAction(executor, FiniteDuration(1, TimeUnit.SECONDS), saveTask)

    text
      .textProperty()
      .addListener((_, oldValue, _) => {
        if (oldValue != "") {
          syncLabel.setText("")
          saveOnIdle.snooze()
        }
      })
  }

  private def saveCommitAndPushTask(l: Label, codeArea: CodeArea, file: File, git: Git): () => Unit = () => {
    Platform.runLater(() => l.setText("Syncing..."))
    Future {
      editorHelper
        .saveCommitAndPush("", codeArea.getText, file, git) match {
        case Right(_) =>
          Platform.runLater(() => {
            l.setText("Sync successful")
          })
        case Left(exception) =>
          Platform.runLater(() => {
            l.setText(s"Push Failed: $exception.uiMessage}")
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
      CancelableAction(executor, FiniteDuration(100, TimeUnit.MILLISECONDS), renderMarkdownTask(text)) // Good settings candidate

    text
      .textProperty()
      .addListener((_, _, _) => {
        markdownTask.snooze()
      })
  }

  private val nodeVisitor: CodeArea => NodeVisitor = (styledText: CodeArea) =>
    new NodeVisitor(new VisitHandler[Text](classOf[Text], (node: Text) => {
      styledText.setStyleClass(node.getStartOffset, node.getEndOffset, "text")
      setParagraphStyle(styledText, node, "inline")
    }), new VisitHandler[Heading](classOf[Heading], (node: Heading) => {
      styledText.setStyleClass(node.getOpeningMarker.getStartOffset, node.getText.getEndOffset, s"h${node.getLevel}")
      setParagraphStyle(styledText, node, "inline")
    }), new VisitHandler[Code](classOf[Code], (node: Code) => {
      styledText.setStyleClass(node.getOpeningMarker.getStartOffset, node.getClosingMarker.getEndOffset, "code")
      setParagraphStyle(styledText, node, "inline")
    }), new VisitHandler[Emphasis](classOf[Emphasis], (node: Emphasis) => {
      styledText.setStyleClass(node.getOpeningMarker.getStartOffset, node.getClosingMarker.getEndOffset, "emphasis")
      setParagraphStyle(styledText, node, "inline")
    }), new VisitHandler[FencedCodeBlock](classOf[FencedCodeBlock], (node: FencedCodeBlock) => {
      if (node.getClosingFence.getEndOffset != 0) {
        styledText.setStyleClass(node.getOpeningMarker.getStartOffset, node.getClosingMarker.getEndOffset, "code-block")
        setParagraphStyle(styledText, node, "code-block")

      }
    }), new VisitHandler[BlockQuote](classOf[BlockQuote], (node: BlockQuote) => {
      styledText.setStyleClass(node.getStartOffset, node.getEndOffset, "quote-block")
      setParagraphStyle(styledText, node, "quote-block")
    }), new VisitHandler[Link](classOf[Link], (node: Link) => {
      styledText
        .setStyleClass(node.getTextOpeningMarker.getStartOffset + 1, node.getTextClosingMarker.getEndOffset - 1, "link")
      styledText
        .setStyleClass(node.getLinkOpeningMarker.getStartOffset + 1, node.getLinkClosingMarker.getEndOffset - 1, "href")
      setParagraphStyle(styledText, node, "inline")
    }))

  private def setParagraphStyle(styledText: CodeArea, node: Node, style: String): Unit = {
    Array
      .range(node.getStartLineNumber, node.getEndLineNumber + 1)
      .foreach(styledText.setParagraphStyle(_, List(style).asJava))
  }
}
