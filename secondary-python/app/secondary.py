import grpc
from concurrent import futures
import time
import logging
from random import randint
import proto.api_pb2_grpc as pb2_grpc
import proto.api_pb2 as pb2


class GrpcLogger(pb2_grpc.LoggerServicer):

    def __init__(self):
        self.items = []

    def ListMessages(self, request, context):
        logging.info(f"Python secondary received ListMessages request")
        response = pb2.ListMessagesResponse(logs=self.items)
        return response

    def AppendMessage(self, request, context):
        logging.info(f"Python secondary received AppendMessages request")
        item = request.log
        response = pb2.AppendMessageResponse(responseCode=0)
        try:
            if item in self.items:
                raise ValueError('Duplicated item')
            else:
                delay = randint(2,10)
                logging.info(f"Python secondary delay for {str(delay)} seconds")
                time.sleep(delay)
                self.items.append(item)
                logging.info(f"Python secondary added new item")
        except:
            response = pb2.AppendMessageResponse(responseCode=1)
            logging.info(f"Python secondary error occurs")
        return response


def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    pb2_grpc.add_LoggerServicer_to_server(GrpcLogger(), server)
    port = 6567
    server.add_insecure_port(f'[::]:{str(port)}')
    logging.info(f"Python secondary started on port {str(port)}")
    server.start()
    try:
        while True:
            logging.info(f"Python secondary is running on port {str(port)}")
            time.sleep(5)
    except KeyboardInterrupt:
        server.stop(0)

if __name__ == '__main__':
    logging.basicConfig()
    logging.getLogger().setLevel(logging.INFO)
    serve()