package org.sisioh.trinity.domain.routing

import com.twitter.finagle.http.Response
import com.twitter.util.Future
import java.util.UUID
import org.jboss.netty.handler.codec.http.HttpMethod
import org.sisioh.dddbase.core.model.{Entity, EntityCloneable, Identity}
import org.sisioh.trinity.domain.http.TrinityRequest
import scala.language.implicitConversions
import org.sisioh.trinity.domain.controller.Controller
import java.util.concurrent.atomic.AtomicLong

/**
 * [[org.sisioh.trinity.domain.routing.Route]]のための識別子。
 *
 * @param method
 * @param pathPattern
 */
case class RouteId(method: HttpMethod, pathPattern: PathPattern)
  extends Identity[(HttpMethod, PathPattern)] {
  def value: (HttpMethod, PathPattern) = (method, pathPattern)
}

/**
 * ルートを表すエンティティ。
 */
trait Route
  extends Entity[RouteId]
  with EntityCloneable[RouteId, Route]
  with Ordered[Route] {

  def compare(that: Route): Int = {
    this.order compareTo that.order
  }

  private val order = Route.orderGenerator.getAndIncrement

  /**
   * コントローラID
   */
  val controllerId: Identity[UUID]

  /**
   * アクション
   */
  val action: Action

  /**
   * アクションを実行する。
   *
   * @param request リクエスト
   * @return `com.twitter.util.Future`
   */
  def apply(request: TrinityRequest): Future[Response] = action(request)

}


/**
 * コンパニオンオブジェクト。
 */
object Route {

  def apply(identity: RouteId, controllerId: Identity[UUID], action: Action): Route =
    new RouteImpl(identity, controllerId, action)

  def apply(identity: RouteId, controller: Controller, action: Action): Route =
    new RouteImpl(identity, controller.identity, action)

  def apply(method: HttpMethod, pathPattern: PathPattern, controller: Controller, action: Action): Route =
    apply(method, pathPattern, controller.identity, action)

  def apply(method: HttpMethod, pathPattern: PathPattern, controllerId: Identity[UUID], action: Action): Route =
    apply(RouteId(method, pathPattern), controllerId, action)

  def apply(method: HttpMethod, path: String, controllerId: Identity[UUID], action: Action)
           (implicit pathPatternParser: PathPatternParser): Route = {
    val pathPattern = pathPatternParser(path)
    apply(RouteId(method, pathPattern), controllerId, action)
  }

  def apply(method: HttpMethod, path: String, controller: Controller, action: Action)
           (implicit pathPatternParser: PathPatternParser): Route =
    apply(method, path, controller.identity, action)

  def unapply(route: Route): Option[(RouteId, Identity[UUID], Action)] =
    Some(route.identity, route.controllerId, route.action)

  private val orderGenerator = new AtomicLong()

}

private[domain]
class RouteImpl(val identity: RouteId, val controllerId: Identity[UUID], val action: Action) extends Route


