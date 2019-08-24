package lockbook.dev

import java.io.File
import java.util.concurrent.{ScheduledFuture, ScheduledThreadPoolExecutor, TimeUnit}

import com.vladsch.flexmark.ast._
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.{Node, NodeVisitor, VisitHandler}
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.control._
import javafx.scene.layout.BorderPane
import javafx.scene.text.{Font, FontPosture}
import org.eclipse.jgit.api.Git
import org.fxmisc.richtext.CodeArea

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EditorUi(editorHelper: EditorHelper) {

  def getView(git: Git, f: File): BorderPane = {
    val textArea  = new CodeArea
    val root      = new BorderPane
    val syncLabel = new Label("Loading File")

    loadFile(git, f, root, textArea)

    BorderPane.setAlignment(syncLabel, Pos.CENTER_RIGHT)
    syncLabel.setId("SyncStatus")
    root.bottomProperty().setValue(syncLabel)
    val autoSaveTask = saveCommitAndPushTask(syncLabel, textArea, f, git)
    scheduleAutoSave(textArea, syncLabel, autoSaveTask)

    root
  }

  private def loadFile(git: Git, f: File, root: BorderPane, text: CodeArea): Future[Unit] = Future {
    editorHelper.getTextFromFile(f) match {
      case Right(fileText) =>
        Platform.runLater(() => {
          doMarkdown(text)
          text.setWrapText(true)
          root.setCenter(text)
          text.replaceText(fileText)
        })
      case Left(error) =>
        Platform.runLater(
          () => root.setCenter(new Label(error.uiMessage))
        )
    }
  }

  private def scheduleAutoSave(text: CodeArea, syncLabel: Label, saveTask: Runnable): Unit = {
    val executor                                = new ScheduledThreadPoolExecutor(1)
    var currentTask: Option[ScheduledFuture[_]] = None

    text
      .textProperty()
      .addListener((_, _, _) => {
        syncLabel.setText("")
        currentTask.foreach(_.cancel(false))
        currentTask = Some(executor.schedule(saveTask, 1, TimeUnit.SECONDS))
      })
  }

  def saveCommitAndPushTask(l: Label, codeArea: CodeArea, file: File, git: Git): Runnable = () => {
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

  private def doMarkdown(text: CodeArea): Unit = {
    val parser: Parser = Parser.builder().build()

    text
      .textProperty()
      .addListener((_, _, newText) => {
        val parsed = parser.parse(newText)
        nodeVisitor(text).visit(parsed)
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
