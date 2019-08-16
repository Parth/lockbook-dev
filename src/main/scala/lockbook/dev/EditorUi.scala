package lockbook.dev

import java.io.File

import com.vladsch.flexmark.ast.{Heading, Text}
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.{Node, NodeVisitor, VisitHandler}
import javafx.application.Platform
import javafx.scene.control._
import javafx.scene.layout.{BorderPane, HBox}
import org.eclipse.jgit.api.Git
import org.fxmisc.richtext.StyleClassedTextArea

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EditorUi(editorHelper: EditorHelper) {

  def getView(git: Git, f: File): BorderPane = {
    val textArea = new StyleClassedTextArea()
    val root     = new BorderPane

    loadFile(git, f, root, textArea)

    root
  }

  private def loadFile(git: Git, f: File, root: BorderPane, text: StyleClassedTextArea): Future[Unit] = Future {
    editorHelper.getTextFromFile(f) match {
      case Right(fileText) =>
        Platform.runLater(() => {
          doMarkdown(text)
          text.setWrapText(true)
          root.setBottom(getBottom(git, f, text))
          root.setCenter(text)
          text.replaceText(fileText)
        })
      case Left(error) =>
        Platform.runLater(
          () => root.setCenter(new Label(error.uiMessage))
        )
    }
  }

  private def doMarkdown(text: StyleClassedTextArea) = {
    val parser: Parser = Parser.builder().build()

    text
      .textProperty()
      .addListener((_, _, newText) => {
        val parsed = parser.parse(newText)
        nodeVisitor(text).visit(parsed)
      })
  }

  private def getBottom(git: Git, file: File, textArea: StyleClassedTextArea): HBox = {
    val save          = new Button("Push")
    val commitMessage = new TextField
    commitMessage.setPromptText("Commit Message")

    save.setOnAction(_ => {
      Future {
        editorHelper
          .saveCommitAndPush(commitMessage.getText, textArea.getText, file, git) match {
          case Right(_) =>
            Platform.runLater(() => {
              AlertUi.showGood("Push Successful", "Changes saved successfully.")
            })
          case Left(exception) =>
            Platform.runLater(() => {
              AlertUi.showBad("Push Failed", exception.uiMessage)
            })
        }
      }
    })

    new HBox(commitMessage, save)
  }

  private val nodeVisitor: StyleClassedTextArea => NodeVisitor = (styledText: StyleClassedTextArea) =>
    new NodeVisitor(new VisitHandler[Heading](classOf[Heading], (node: Heading) => {
      styledText.setStyleClass(node.getOpeningMarker.getStartOffset, node.getText.getEndOffset, s"h${node.getLevel}")
    }), new VisitHandler[Text](classOf[Text], (node: Text) => {
      styledText.setStyleClass(node.getStartOffset, node.getEndOffset, "")
    }))
}
