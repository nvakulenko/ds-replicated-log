import grpc
from concurrent import futures
import time
import proto.api_pb2_grpc as pb2_grpc
import proto.api_pb2 as pb2


class GrpcLogger(pb2_grpc.LoggerServicer):

    def __init__(self):
        self.items = []

    def ListMessages(self, request, context):
        response = pb2.ListMessagesResponse(logs=self.items)
        return response

    def AppendMessage(self, request, context):
        item = request.log
        self.items.append(item)
        return pb2.AppendMessageResponse()


if __name__ == '__main__':
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    pb2_grpc.add_LoggerServicer_to_server(GrpcLogger(), server)
    server.add_insecure_port('[::]:50052')
    server.start()
    try:
        while True:
            time.sleep(5)
    except KeyboardInterrupt:
        server.stop(0)
