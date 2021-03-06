import grpc
from concurrent import futures
import time
import logging
from random import randint
import proto.api_pb2_grpc as pb2_grpc
import proto.api_pb2 as pb2
from collections import OrderedDict 


class GrpcLogger(pb2_grpc.LoggerServicer):

    def __init__(self):
        self.items = OrderedDict([(0 , pb2.LogMessage(log="Zero log from Python Secondary"))])
        self.buffer = dict()

    def ListMessages(self, request, context):
        logging.info(f"Python secondary received ListMessages request")
        if self.buffer:
            print(self.buffer.keys())
            exp_item_id = max(self.items, key=int) + 1
            while exp_item_id in self.buffer.keys():
                new_val = self.buffer.pop(exp_item_id)
                self.items[exp_item_id] = new_val
                exp_item_id += 1
        response = pb2.ListMessagesResponse(logs=list(self.items.values()))
        return response

    def AppendMessage(self, request, context):
        item = request.log
        logging.info(f"Python secondary received AppendMessages request {str(item.log)}")
        response = pb2.AppendMessageResponse(responseCode=0)
        try:
            if item.id in self.items.keys() or item.id in self.buffer.keys():
                raise ValueError('Duplicated item')
            else:
                delay = randint(2,10)
                logging.info(f"Python secondary delay for {str(delay)} seconds")
                time.sleep(delay)
                if item.id == max(self.items, key=int) + 1:
                    self.items[item.id] = item
                else:
                    self.buffer[item.id] = item
                logging.info(f"Python secondary added new item")
        except:
            response = pb2.AppendMessageResponse(responseCode=2,responseMessage=f"Finishing processing log with id {str(item.id)} by randomly generated error")
            logging.info(f"Python secondary error occurs")
        return response

    def HealthCheck(self, request, context):
        logging.info(f"Python secondary received HealthCheck request")
        health_response = pb2.HealthCheckResponse(status=0)
        return health_response

def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    pb2_grpc.add_LoggerServicer_to_server(GrpcLogger(), server)
    port = 6567
    server.add_insecure_port(f'[::]:{str(port)}')
    logging.info(f"Python secondary started on port {str(port)}")
    server.start()
    try:
        while True:
            time.sleep(5)
    except KeyboardInterrupt:
        server.stop(0)

if __name__ == '__main__':
    logging.basicConfig()
    logging.getLogger().setLevel(logging.INFO)
    serve()